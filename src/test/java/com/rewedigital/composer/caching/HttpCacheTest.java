package com.rewedigital.composer.caching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.google.common.testing.FakeTicker;
import com.google.inject.Provider;
import com.spotify.apollo.Environment;
import com.spotify.apollo.Request;
import com.spotify.apollo.Response;
import com.spotify.apollo.Status;
import com.spotify.apollo.StatusType;
import com.spotify.apollo.environment.IncomingRequestAwareClient;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import okio.ByteString;

public class HttpCacheTest {

    private final FakeTicker ticker = new FakeTicker();

    @Test
    public void doesNotCacheIfCacheHeaderNoStore() {
        final HttpCache cache = new HttpCache(env());
        final IncomingRequestAwareClient client = aClientReturning(aResponseWith("no-store"));
        final Request request = Request.forUri("/");

        cache.withCaching(request, Optional.empty(), client);
        cache.withCaching(request, Optional.empty(), client);

        verify(client, times(2)).send(request, Optional.empty());
    }

    @Test
    public void doesCacheIfCacheHeaderAllowesIt() {
        final HttpCache cache = new HttpCache(env());
        final Response<ByteString> response = aResponseWith("max-age=100");
        final IncomingRequestAwareClient client = aClientReturning(response);

        final Request request = Request.forUri("/");
        cache.withCaching(request, Optional.empty(), client);
        cache.withCaching(request, Optional.empty(), client);

        verify(client, times(1)).send(request, Optional.empty());
    }

    @Test
    public void returnsCachedResponse() throws Exception {
        final HttpCache cache = new HttpCache(env());
        final Response<ByteString> response = aResponseWith("max-age=100");
        final IncomingRequestAwareClient client = aClientReturning(response);

        final Request request = Request.forUri("/");
        cache.withCaching(request, Optional.empty(), client);
        final Response<ByteString> actualResponse =
            cache.withCaching(request, Optional.empty(), client).toCompletableFuture().get();

        assertThat(actualResponse).isEqualTo(response);
    }

    @Test
    public void expiresCachedResponse() {
        final HttpCache cache = new HttpCache(env(), ticker::read);
        final IncomingRequestAwareClient client = aClientReturning(aResponseWith("max-age=100"));
        final Request request = Request.forUri("/");

        cache.withCaching(request, Optional.empty(), client);
        ticker.advance(200, TimeUnit.SECONDS);
        cache.withCaching(request, Optional.empty(), client);

        verify(client, times(2)).send(request, Optional.empty());
    }

    @Test
    public void considersQueryParametersInCacheKey() {
        final HttpCache cache = new HttpCache(env());
        final IncomingRequestAwareClient client = aClientReturning(aResponseWith("max-age=100"));
        final Request firstRequest = Request.forUri("/?q=1");
        final Request secondRequest = Request.forUri("/?q=2");

        cache.withCaching(firstRequest, Optional.empty(), client);
        cache.withCaching(secondRequest, Optional.empty(), client);

        verify(client, times(1)).send(firstRequest, Optional.empty());
        verify(client, times(1)).send(secondRequest, Optional.empty());
    }

    @Test
    public void cacheIsDisabledViaConfiguration() {
        final HttpCache cache = new HttpCache(envWith(config()
            .withValue("composer.http.cache.enabled", ConfigValueFactory.fromAnyRef(false))));
        final IncomingRequestAwareClient client = aClientReturning(aResponseWith("max-age=100"));
        final Request request = Request.forUri("/");

        cache.withCaching(request, Optional.empty(), client);
        cache.withCaching(request, Optional.empty(), client);

        verify(client, times(2)).send(request, Optional.empty());
    }

    @Test
    public void doesNotFailOnNullResponse() {
        final HttpCache cache = new HttpCache(env());
        final IncomingRequestAwareClient client = aClientReturning(null);
        final Request request = Request.forUri("/");

        cache.withCaching(request, Optional.empty(), client);
    }

    @Test
    public void bypassesCacheIfRequestSendsNoCacheHeader() {
        final HttpCache cache = new HttpCache(env());
        final IncomingRequestAwareClient client = aClientReturning(aResponseWith("max-age=100"));
        final Request request = Request.forUri("/");
        cache.withCaching(request, Optional.empty(), client);

        final Request noCacheRequest = request.withHeader("Cache-Control", "no-cache");
        cache.withCaching(noCacheRequest, Optional.empty(), client);

        verify(client, times(1)).send(request, Optional.empty());
        verify(client, times(1)).send(noCacheRequest, Optional.empty());
    }

    @Test
    public void onlyCachesResponsesToGetAndHeadRequests() {
        final Map<String, Integer> methodToCalls = new HashMap<String, Integer>() {
            private static final long serialVersionUID = 1L;
            {
                put("GET", 1);
                put("HEAD", 1);
                put("OPTION", 2);
                put("POST", 2);
                put("PUT", 2);
                put("DELETE", 2);
            }
        };

        final HttpCache cache = new HttpCache(env());
        final IncomingRequestAwareClient client = aClientReturning(aResponseWith("max-age=100"));

        for (Entry<String, Integer> m : methodToCalls.entrySet()) {
            Request request = Request.forUri("/", m.getKey());
            cache.withCaching(request, Optional.empty(), client);
            cache.withCaching(request, Optional.empty(), client);

            verify(client, times(m.getValue())).send(request, Optional.empty());
        }
    }

    @Test
    public void onlyCachesResponsesWithCacheableStatusCode() {
        final Map<Status, Integer> statusToCalls = new HashMap<Status, Integer>() {
            private static final long serialVersionUID = 1L;
            {
                put(Status.BAD_GATEWAY, 2);
                put(Status.BAD_REQUEST, 2);
                put(Status.CREATED, 2);
                put(Status.FORBIDDEN, 2);
                put(Status.INTERNAL_SERVER_ERROR, 2);
                put(Status.SEE_OTHER, 2);
                put(Status.TEMPORARY_REDIRECT, 2);

                put(Status.OK, 1);
                put(Status.GONE, 1);
                put(Status.MOVED_PERMANENTLY, 1);
            }
        };

        final HttpCache cache = new HttpCache(env());
        for (Entry<Status, Integer> m : statusToCalls.entrySet()) {
            final IncomingRequestAwareClient client = aClientReturning(aResponseWith(m.getKey(), "max-age=100"));

            Request request = Request.forUri("/" + m.getKey(), "GET");
            cache.withCaching(request, Optional.empty(), client);
            cache.withCaching(request, Optional.empty(), client);

            verify(client, times(m.getValue())).send(request, Optional.empty());
        }
    }

    private Response<ByteString> aResponseWith(final String value) {
        return aResponseWith(Status.OK, value);
    }

    private Response<ByteString> aResponseWith(StatusType status, final String value) {
        return Response.of(status, ByteString.EMPTY).withHeader("Cache-Control", value);
    }

    private IncomingRequestAwareClient aClientReturning(final Response<ByteString> response) {
        final IncomingRequestAwareClient client = mock(IncomingRequestAwareClient.class);
        when(client.send(any(), any())).thenReturn(CompletableFuture.completedFuture(response));
        return client;
    }

    private static Provider<Environment> env() {
        return envWith(config().withValue("composer.http.cache.enabled", ConfigValueFactory.fromAnyRef(true))
            .withValue("composer.http.cache.size", ConfigValueFactory.fromAnyRef(10_000)));
    }

    private static Provider<Environment> envWith(final Config config) {
        final Environment environment = mock(Environment.class);
        when(environment.config()).thenReturn(config);
        return () -> environment;
    }

    private static Config config() {
        return ConfigFactory.empty();
    }
}
