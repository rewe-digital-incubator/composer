package com.rewedigital.composer.caching;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.Ticker;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.spotify.apollo.Environment;
import com.spotify.apollo.Request;
import com.spotify.apollo.Response;
import com.spotify.apollo.environment.IncomingRequestAwareClient;
import com.spotify.ffwd.http.okhttp3.CacheControl;
import com.typesafe.config.Config;

import okio.ByteString;

public class HttpCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpCache.class);

    private final Cache<String, Response<ByteString>> cache;
    final boolean enabled;

    @Inject
    public HttpCache(final Provider<Environment> environmentProvider) {
        this(environmentProvider, Ticker.systemTicker());
    }

    @VisibleForTesting
    HttpCache(final Provider<Environment> environmentProvider, final Ticker ticker) {
        final Config config = environmentProvider.get().config();
        enabled = config.getBoolean("composer.http.cache.enabled");
        if (!enabled) {
            cache = null;
            return;
        }

        final int size = config.getInt("composer.http.cache.size");
        cache = Caffeine.newBuilder()
            .maximumSize(size)
            .ticker(ticker)
            .expireAfter(cacheHeaderBasedExpiry())
            .build();
    }

    public CompletionStage<Response<ByteString>> withCaching(final Request request, final Optional<Request> incoming,
        final IncomingRequestAwareClient client) {
        if (!enabled) {
            return client.send(request, incoming);
        }

        final String cacheKey = request.uri();
        return queryCache(cacheKey)
            .map(returnCachedResponse(cacheKey))
            .orElseGet(fetchFromUpstream(request, incoming, client, cacheKey));
    }

    private Optional<Response<ByteString>> queryCache(final String cacheKey) {
        LOGGER.debug("querying cache for {}", cacheKey);
        return Optional.ofNullable(cache.getIfPresent(cacheKey));
    }

    private Function<Response<ByteString>, CompletionStage<Response<ByteString>>> returnCachedResponse(
        final String cacheKey) {
        return response -> {
            LOGGER.debug("serving response for cache key {} from cache (response: {})", cacheKey, response);
            return CompletableFuture.completedFuture(response);
        };
    }

    private Supplier<CompletionStage<Response<ByteString>>> fetchFromUpstream(final Request request,
        final Optional<Request> incoming, final IncomingRequestAwareClient client, final String cacheKey) {
        return () -> client
            .send(request, incoming)
            .whenComplete((response, ex) -> {
                LOGGER.debug("fetched response for cache key {} (response: {})", cacheKey, response);
                cacheIfAdmissible(cacheKey, response);
            });
    }

    private void cacheIfAdmissible(final String cacheKey, final Response<ByteString> response) {
        final CacheControl cacheControl = CacheHeaders.of(response);
        if (isAdmissibleForCaching(cacheControl)) {
            LOGGER.debug("caching response for cache key {}, max age: {}", cacheKey, cacheControl.maxAgeSeconds());
            cache.put(cacheKey, response);
        }
    }

    private boolean isAdmissibleForCaching(final CacheControl cacheControl) {
        return !cacheControl.noCache() && !cacheControl.noStore() && cacheControl.maxAgeSeconds() > 0;
    }

    private static Expiry<String, Response<ByteString>> cacheHeaderBasedExpiry() {
        return new Expiry<String, Response<ByteString>>() {

            @Override
            public long expireAfterCreate(final String key, final Response<ByteString> value,
                final long currentTime) {
                return expiration(value);
            }

            @Override
            public long expireAfterRead(final String key, final Response<ByteString> value,
                final long currentTime, final long currentDuration) {
                return currentDuration;
            }

            @Override
            public long expireAfterUpdate(final String key, final Response<ByteString> value,
                final long currentTime, final long currentDuration) {
                return expiration(value);
            }
        };
    }

    private static long expiration(final Response<ByteString> response) {
        final CacheControl cacheControl = CacheHeaders.of(response);
        return TimeUnit.SECONDS.toNanos(cacheControl.maxAgeSeconds());
    }
}
