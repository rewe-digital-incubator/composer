package com.rewedigital.composer.client;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;
import org.mockito.Mockito;

import com.spotify.apollo.Request;
import com.spotify.apollo.environment.IncomingRequestAwareClient;

public class ProxyHeaderClientDecoratorTest {

    private final List<String> hopByHopHeaders = Arrays.asList("Connection", "Keep-Alive", "Proxy-Authenticate",
        "Proxy-Authorization", "TE", "Trailer", "Transfer-Encoding", "Upgrade");

    private final ProxyHeaderClientDecorator decorator = new ProxyHeaderClientDecorator();

    @Test
    public void shouldRemoveAllHopByHopHeaders() {
        final IncomingRequestAwareClient client = mock(IncomingRequestAwareClient.class);
        final Request request = Request.forUri("upstream/");
        final Optional<Request> incoming = Optional.of(withHopByHopHeaders(Request.forUri("/")));

        decorator.apply(client).send(request, incoming);
        verify(client).send(aRequestWithout(hopByHopHeaders), eq(incoming));
    }

    @Test
    public void shouldAttachEndToEndHeadersToUpstreamRequest() {
        final IncomingRequestAwareClient client = mock(IncomingRequestAwareClient.class);
        final Request request = Request.forUri("upstream/");
        final Optional<Request> incoming = Optional.of(Request.forUri("/").withHeader("Cache-Control", "no-cache"));

        decorator.apply(client).send(request, incoming);
        verify(client).send(aRequestWithHeader("Cache-Control", "no-cache"), eq(incoming));
    }

    @Test
    public void shouldAttachForwardedPathToUpstreamRequest() {
        final IncomingRequestAwareClient client = mock(IncomingRequestAwareClient.class);
        final Request request = Request.forUri("upstream/");
        final Optional<Request> incoming = Optional.of(Request.forUri("/some/path"));

        decorator.apply(client).send(request, incoming);
        verify(client).send(aRequestWithHeader("x-forwarded-path", "/some/path"), eq(incoming));
    }

    private Request withHopByHopHeaders(final Request request) {
        Request result = request;
        for (final String header : hopByHopHeaders) {
            result = result.withHeader(header, header);
        }
        return result;
    }

    private static Request aRequestWithout(final List<String> headerNames) {
        return Mockito.argThat(new TypeSafeMatcher<Request>() {

            @Override
            public void describeTo(final Description description) {
                description.appendText("request without").appendValueList(" headers named {", ",", "}", headerNames);
            }

            @Override
            protected boolean matchesSafely(final Request item) {
                for (final String headerName : headerNames) {
                    if (item.header(headerName).isPresent()) {
                        return false;
                    }
                }
                return true;
            }
        });
    }

    private static Request aRequestWithHeader(final String name, final String value) {
        return Mockito.argThat(new TypeSafeMatcher<Request>() {

            @Override
            public void describeTo(final Description description) {
                description.appendText("Request with header {").appendValue(name).appendText("=")
                    .appendValue(value).appendText("}");
            }

            @Override
            protected boolean matchesSafely(final Request item) {
                return item.header(name).equals(Optional.ofNullable(value));
            }
        });
    }

}
