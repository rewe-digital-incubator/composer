package com.rewedigital.composer.application;

import static com.rewedigital.composer.routing.RouteTypeName.PROXY;
import static com.rewedigital.composer.routing.RouteTypeName.TEMPLATE;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.junit.Test;

import com.rewedigital.composer.composing.ComposingResponse;
import com.rewedigital.composer.composing.ResponseComposition;
import com.rewedigital.composer.composing.ResponseCompositionHandler;
import com.rewedigital.composer.routing.BackendRouting;
import com.rewedigital.composer.routing.CompositionAwareRequestClient;
import com.rewedigital.composer.routing.Match;
import com.rewedigital.composer.routing.RouteMatch;
import com.rewedigital.composer.routing.RouteTypeName;
import com.rewedigital.composer.routing.RouteTypes;
import com.spotify.apollo.Client;
import com.spotify.apollo.Request;
import com.spotify.apollo.RequestContext;
import com.spotify.apollo.Response;
import com.spotify.apollo.Status;
import com.spotify.apollo.route.Rule;

import okio.ByteString;

public class ComposingRequestHandlerTest {

    private static final String SERVICE_RESPONSE = "<html>test</html>";

    @Test
    public void returnsResponseFromTemplateRoute() throws Exception {
        final RequestHandler handler = new RequestHandler(aRouter("/<path:path>", TEMPLATE),
                RoutingResult.returning(Status.OK, SERVICE_RESPONSE),
                sessionSerializer());

        final Response<ByteString> response = handler.execute(aContext()).toCompletableFuture().get();

        assertThat(response.payload().get().utf8()).isEqualTo(SERVICE_RESPONSE);
    }

    @Test
    public void returnsDefaultResponseFromErrorOnTemplateRoute() throws Exception {
        final RequestHandler handler = new RequestHandler(aRouter("/<path:path>", TEMPLATE),
                RoutingResult.returning(Status.BAD_REQUEST, ""),
                sessionSerializer());

        final Response<ByteString> response = handler.execute(aContext()).toCompletableFuture().get();
        assertThat(response.status()).isEqualTo(Status.INTERNAL_SERVER_ERROR);
    }

    @Test
    public void returnsErrorResponseFromProxyRoute() throws Exception {
        final RequestHandler handler = new RequestHandler(aRouter("/<path:path>", PROXY),
                RoutingResult.returning(Status.BAD_REQUEST, ""),
                sessionSerializer());

        final Response<ByteString> response = handler.execute(aContext()).toCompletableFuture().get();
        assertThat(response.status()).isEqualTo(Status.BAD_REQUEST);

    }

    private BackendRouting aRouter(final String pattern, final RouteTypeName routeType) {
        final Rule<Match> sampleRule = Rule.fromUri(pattern, "GET", Match.of("http://target", routeType));
        return new BackendRouting(singletonList(sampleRule));
    }

    private RequestContext aContext() {
        final Request request = mock(Request.class);
        when(request.uri()).thenReturn("http://uri.de/hello");
        when(request.service()).thenReturn(Optional.empty());
        when(request.method()).thenReturn("GET");
        final RequestContext context = mock(RequestContext.class);
        when(context.request()).thenReturn(request);
        final Client client = mock(Client.class);
        when(client.send(any())).thenThrow(new RuntimeException());
        when(context.requestScopedClient()).thenReturn(client);
        return context;
    }

    private ResponseCompositionHandler sessionSerializer() {
        return new ResponseCompositionHandler() {

            @Override
            public CompletionStage<ResponseComposition> initializeFrom(final RequestContext context) {
                return CompletableFuture.completedFuture(ResponseComposition.of(Collections.emptyList()));
            }
        };

    }

    private static class RoutingResult extends CompositionAwareRequestClient {

        public static RouteTypes returning(final Status status, final String responseBody) {
            return new RouteTypes(composerFactory(), new CompositionAwareRequestClient() {
                @Override
                public CompletionStage<ComposingResponse<ByteString>> fetch(final RouteMatch rm,
                        final RequestContext context, final ResponseComposition extension) {
                    return CompletableFuture.completedFuture(
                            ComposingResponse.of(Response.of(status, ByteString.encodeUtf8(responseBody)),
                                    extension));
                }
            });
        }

        private static ComposerFactory composerFactory() {
            return new ComposerFactory(DefaultConfiguration.defaultConfiguration().getConfig("composer.html"));
        }
    }

}
