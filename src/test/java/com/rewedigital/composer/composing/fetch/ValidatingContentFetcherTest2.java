package com.rewedigital.composer.composing.fetch;

import static java.util.Collections.emptyMap;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static okio.ByteString.encodeUtf8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import com.rewedigital.composer.composing.CompositionStep;
import com.rewedigital.composer.composing.fetch.FetchContext;
import com.rewedigital.composer.composing.fetch.ValidatingContentFetcher;
import com.rewedigital.composer.session.SessionRoot;
import com.spotify.apollo.Client;
import com.spotify.apollo.Request;
import com.spotify.apollo.Response;
import com.spotify.apollo.test.StubClient;

import okio.ByteString;

public class ValidatingContentFetcherTest2 {

    private final SessionRoot emptySession = SessionRoot.empty();
    private final Duration timeOut = Duration.ofMillis(222);

    @Test
    public void fetchesEmptyContentForMissingPath() throws Exception {
        final Response<String> result =
            new ValidatingContentFetcher(mock(Client.class), emptyMap(), emptySession)
                .fetch(FetchContext.of(null, null, timeOut), aStep())
                .get();
        assertThat(result.payload()).contains("");
    }

    @Test
    public void fetchesContentFromPath() throws Exception {
        final Client client = mock(Client.class);
        when(client.send(aRequestWithPath("/some/path"))).thenReturn(aResponse("ok"));
        final Response<String> result =
            new ValidatingContentFetcher(client, emptyMap(), emptySession)
                .fetch(FetchContext.of("/some/path", "fallback", timeOut), aStep())
                .get();
        assertThat(result.payload()).contains("ok");
    }

    @Test
    public void expandsPathWithParameters() throws Exception {
        final Client client = mock(Client.class);
        when(client.send(aRequestWithPath("/some/path/val"))).thenReturn(aResponse("ok"));
        final Response<String> result =
            new ValidatingContentFetcher(client, params("var", "val"), emptySession)
                .fetch(FetchContext.of("/some/path/{var}", "fallback", timeOut), aStep())
                .get();
        assertThat(result.payload()).contains("ok");
    }

    @Test
    public void defaultsNonHTMLResponsesToEmpty() throws Exception {
        final Client client = mock(Client.class);
        when(client.send(aRequestWithPath("/some/path"))).thenReturn(aResponse("ok", "text/json"));
        final Response<String> result =
            new ValidatingContentFetcher(client, emptyMap(), emptySession)
                .fetch(FetchContext.of("/some/path", "", timeOut), aStep())
                .get();
        assertThat(result.payload()).contains("");
    }

    @Test
    public void forwardsSession() throws Exception {
        final Client client = mock(Client.class);
        when(client.send(any())).thenReturn(aResponse(""));
        final SessionRoot session = session("x-rd-key", "value");
        new ValidatingContentFetcher(client, emptyMap(), session)
            .fetch(FetchContext.of("/some/path", "", timeOut), aStep())
            .get();
        verify(client).send(aRequestWithSession("x-rd-key", "value"));
    }

    @Test
    public void validatingContentFetcherAppliesTtl() throws Exception {
        final StubClient client = aStubClient();

        new ValidatingContentFetcher(client, emptyMap(), emptySession)
            .fetch(FetchContext.of("path", "fallback", timeOut), aStep())
            .get();

        assertThat(client.sentRequests()).isNotEmpty().allSatisfy(r -> ttlIsSet(r));
    }

    private void ttlIsSet(final Request request) {
        assertThat(request.ttl()).isNotEmpty();
        assertThat(request.ttl().get()).isEqualTo(timeOut);
    }

    private StubClient aStubClient() {
        final StubClient client = new StubClient();
        final Response<ByteString> emptyResponse =
            Response.forPayload(ByteString.EMPTY).withHeader("Content-Type", "text/html");
        client.respond(emptyResponse).to("path");
        return client;
    }

    private static Request aRequestWithPath(final String path) {
        return argThat(new ArgumentMatcher<Request>() {

            @Override
            public boolean matches(final Object argument) {
                return Request.class.isAssignableFrom(argument.getClass()) &&
                    path.equals(((Request) argument).uri());
            }
        });
    }

    private static Request aRequestWithSession(final String key, final String value) {
        return argThat(new ArgumentMatcher<Request>() {

            @Override
            public boolean matches(final Object argument) {
                return Request.class.isAssignableFrom(argument.getClass())
                    && ((Request) argument).header(key).equals(Optional.of(value));
            }
        });
    }

    private static CompletionStage<Response<ByteString>> aResponse(final String payload) {
        return aResponse(payload, "text/html");
    }

    private static CompletionStage<Response<ByteString>> aResponse(final String payload, final String contentType) {
        return completedFuture(
            Response.forPayload(encodeUtf8(payload)).withHeader("Content-Type", contentType));
    }

    private static CompositionStep aStep() {
        return CompositionStep.root("some-path");
    }

    private static SessionRoot session(final String key, final String value) {
        return SessionRoot.of(params(key, value));
    }

    private static <T> Map<String, T> params(final String name, final T value) {
        final Map<String, T> params = new HashMap<>();
        params.put(name, value);
        return params;
    }
}
