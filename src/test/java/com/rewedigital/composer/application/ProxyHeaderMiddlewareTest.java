package com.rewedigital.composer.application;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.time.Instant;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.ArgumentMatcher;

import com.rewedigital.composer.helper.ARequest;
import com.spotify.apollo.Request;
import com.spotify.apollo.RequestContext;
import com.spotify.apollo.Response;
import com.spotify.apollo.route.AsyncHandler;
import okio.ByteString;

public class ProxyHeaderMiddlewareTest {

    private static final String[] hopByHopHeaders = {"Connection", "Keep-Alive", "Proxy-Authenticate",
        "Proxy-Authorization", "TE", "Trailer", "Transfer-Encoding", "Upgrade"};

    @Test
    public void shouldRemoveAllHopByHopHeaders() {
        @SuppressWarnings("unchecked") final AsyncHandler<Response<ByteString>> innerHandler = mock(AsyncHandler.class);
        final Request original = withHopByHopHeaders(Request.forUri("/"));
        ProxyHeaderMiddleware.apply(innerHandler).invoke(aContextFor(original));
        verify(innerHandler).invoke(aContextWith(ARequest.withoutHeader(hopByHopHeaders)));
    }

    @Test
    public void shouldKeepEndToEndHeaders() {
        @SuppressWarnings("unchecked") final AsyncHandler<Response<ByteString>> innerHandler = mock(AsyncHandler.class);
        final Request original = Request.forUri("/").withHeader("Cache-Control", "no-cache");
        ProxyHeaderMiddleware.apply(innerHandler).invoke(aContextFor(original));
        verify(innerHandler).invoke(aContextWith(ARequest.withHeader("Cache-Control", "no-cache")));
    }

    @Test
    public void shouldAttachForwardedPath() {
        @SuppressWarnings("unchecked") final AsyncHandler<Response<ByteString>> innerHandler = mock(AsyncHandler.class);
        final Request original = Request.forUri("/some/path");
        ProxyHeaderMiddleware.apply(innerHandler).invoke(aContextFor(original));
        verify(innerHandler).invoke(aContextWith(ARequest.withHeader("x-forwarded-path", "/some/path")));
    }

    private Request withHopByHopHeaders(final Request request) {
        Request result = request;
        for (final String header : hopByHopHeaders) {
            result = result.withHeader(header, header);
        }
        return result;
    }

    private static RequestContext aContextFor(final Request request) {
        final RequestContext context = mock(RequestContext.class, withSettings().defaultAnswer(RETURNS_DEEP_STUBS));
        when(context.request()).thenReturn(request);
        when(context.metadata().arrivalTime()).thenReturn(Instant.now());
        return context;
    }

    private RequestContext aContextWith(final ArgumentMatcher<Request> request) {
        return Mockito.argThat(item -> request.matches(item.request()));
    }
}
