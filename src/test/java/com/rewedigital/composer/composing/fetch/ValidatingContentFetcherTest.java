package com.rewedigital.composer.composing.fetch;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;

import com.rewedigital.composer.composing.CompositionStep;
import com.rewedigital.composer.session.SessionRoot;
import com.spotify.apollo.Request;
import com.spotify.apollo.Response;
import com.spotify.apollo.test.StubClient;

import okio.ByteString;

public class ValidatingContentFetcherTest {

    private static final Duration TIME_OUT = Duration.ofMillis(222);

    @Test
    public void validatingContentFetcherAppliesTtl() {
        final StubClient client = aClient();

        final ValidatingContentFetcher fetcher =
            new ValidatingContentFetcher(client, Collections.emptyMap(), SessionRoot.empty());

        final CompletableFuture<Response<String>> response =
            fetcher.fetch(FetchContext.of("path", "fallback", TIME_OUT), CompositionStep.root(""));

        assertThat(response).isDone();
        assertThat(client.sentRequests()).isNotEmpty().allMatch(r -> ttlIsSet(r));
    }

    private boolean ttlIsSet(final Request r) {
        return r.ttl().isPresent() && r.ttl().get() == TIME_OUT;
    }

    private StubClient aClient() {
        final StubClient client = new StubClient();
        final Response<ByteString> emptyResponse =
            Response.forPayload(ByteString.EMPTY).withHeader("Content-Type", "text/html");
        client.respond(emptyResponse).to("path");
        return client;
    }

}
