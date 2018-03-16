package com.rewedigital.composer.client;

import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.Test;

import com.rewedigital.composer.helper.RequestMatching;
import com.spotify.apollo.Request;
import com.spotify.apollo.environment.IncomingRequestAwareClient;

public class WithIncomingHeadersClientDecoratorTest {

    private final WithIncomingHeadersClientDecorator decorator = new WithIncomingHeadersClientDecorator();

    @Test
    public void shouldForwardIncomingHeadersToUpstreamRequest() {
        final IncomingRequestAwareClient client = mock(IncomingRequestAwareClient.class);
        final Request request = Request.forUri("upstream/");
        final Optional<Request> incoming = Optional.of(Request.forUri("/").withHeader("Cache-Control", "no-cache"));

        decorator.apply(client).send(request, incoming);
        verify(client).send(argThat(RequestMatching.withHeader("Cache-Control", "no-cache")), eq(incoming));
    }

    @Test
    public void shouldNotForwardIncomingSessionHeadersToUpstreamRequest() {
        final IncomingRequestAwareClient client = mock(IncomingRequestAwareClient.class);
        final Request request = Request.forUri("upstream/");
        final Optional<Request> incoming = Optional.of(Request.forUri("/").withHeader("x-rd-secret-session", "value"));

        decorator.apply(client).send(request, incoming);
        verify(client).send(argThat(RequestMatching.withoutHeader("x-rd-secret-session")), eq(incoming));
    }
}
