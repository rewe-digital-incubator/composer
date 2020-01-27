package com.rewedigital.composer.session;

import static com.rewedigital.composer.helper.Sessions.sessionRoot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.junit.Test;

import com.rewedigital.composer.helper.ARequest;
import com.spotify.apollo.Client;
import com.spotify.apollo.RequestContext;
import com.spotify.apollo.Response;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import okio.ByteString;

public class RemoteHttpSessionInterceptorTest {

    @Test
    public void shouldCallConfiguredUrlToUpdateSession() throws Exception {
        final String url = "https://test.uri/session";
        final RemoteHttpSessionInterceptor interceptor =
            new RemoteHttpSessionInterceptor(config().withValue("url", ConfigValueFactory.fromAnyRef(url)));

        final Client client = mock(Client.class);
        when(client.send(argThat(ARequest.with("POST", url, "{\"x-rd-key\":\"value\"}"))))
            .thenReturn(response("{\"x-rd-key\":\"new-value\"}"));

        final SessionRoot session =
            interceptor.afterCreation(sessionRoot("x-rd-key", "value"), contextWith(client))
                .toCompletableFuture().get();
        assertThat(session.get("key")).contains("new-value");
    }

    private CompletionStage<Response<ByteString>> response(final String payload) {
        return CompletableFuture.completedFuture(Response.forPayload(ByteString.encodeUtf8(payload)));
    }

    private Config config() {
        return ConfigFactory.empty();
    }

    private static RequestContext contextWith(final Client client) {
        final RequestContext context = mock(RequestContext.class);
        when(context.requestScopedClient()).thenReturn(client);
        return context;
    }

}
