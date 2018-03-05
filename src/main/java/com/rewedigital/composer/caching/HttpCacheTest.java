package com.rewedigital.composer.caching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    private Response<ByteString> aResponseWith(final String value) {
        return Response.of(Status.OK, ByteString.EMPTY).withHeader("Cache-Control", value);
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
