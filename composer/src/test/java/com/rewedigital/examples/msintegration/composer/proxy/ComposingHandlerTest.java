package com.rewedigital.examples.msintegration.composer.proxy;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import org.junit.Test;

import com.rewedigital.examples.msintegration.composer.composing.Composer;
import com.rewedigital.examples.msintegration.composer.routing.BackendRouting;
import com.rewedigital.examples.msintegration.composer.routing.BackendRouting.RouteMatch;
import com.spotify.apollo.Environment;
import com.spotify.apollo.Request;
import com.spotify.apollo.RequestContext;
import com.spotify.apollo.Response;
import com.spotify.apollo.Status;
import com.spotify.apollo.route.Rule;
import com.spotify.apollo.route.RuleRouter;

import okio.ByteString;

public class ComposingHandlerTest {

    private static final String SERVICE_RESPONSE = "<html>test</html>";

    @Test
    public void happyPathSuccess() throws InterruptedException, ExecutionException {
        final ComposingRequestHandler handler = new ComposingRequestHandler(new BackendRouting(aRouter()), new StubTemplateClient(),
            new Composer(mock(Environment.class)));

        final Response<String> response = handler.execute(aContext()).toCompletableFuture().get();

        assertThat(response.payload().get()).isEqualTo(SERVICE_RESPONSE);
    }

    private RuleRouter<String> aRouter() {
        return RuleRouter.of(singletonList(Rule.fromUri("/<path:path>", "GET", "http://target")));
    }

    private RequestContext aContext() {
        final Request request = mock(Request.class);
        when(request.uri()).thenReturn("http://uri.de/hello");
        when(request.service()).thenReturn(Optional.empty());
        when(request.method()).thenReturn("GET");
        final RequestContext context = mock(RequestContext.class);
        when(context.request()).thenReturn(request);
        return context;
    }

    private static class StubTemplateClient extends TemplateClient {

        @Override
        public CompletionStage<Response<ByteString>> getTemplate(final RouteMatch match, final Request request,
            final RequestContext context) {
            return CompletableFuture.completedFuture(Response.of(Status.OK, ByteString.encodeUtf8(SERVICE_RESPONSE)));
        }

    }

}