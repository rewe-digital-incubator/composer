package com.rewedigital.composer.composing;

import static com.rewedigital.composer.composing.CompositionStep.root;
import static java.util.Optional.empty;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.Test;

import com.spotify.apollo.Response;
import com.spotify.apollo.Status;

public class IncludedServiceTest {

    private final ContentFetcher fetcher =
        mock(ContentFetcher.class, withSettings().defaultAnswer(RETURNS_DEEP_STUBS));
    private final ContentComposer composer =
        mock(ContentComposer.class, withSettings().defaultAnswer(RETURNS_DEEP_STUBS));
    private final Composition composition =
        mock(Composition.class, withSettings().defaultAnswer(RETURNS_DEEP_STUBS));

    @Test
    public void fetchesContentFromPath() {
        final IncludedService service = new IncludedService.Builder()
            .attribute("path", "some/path")
            .fallback("some fallback")
            .build();

        service.fetch(fetcher, root("/"));

        verify(fetcher).fetch(eq(FetchContext.of("some/path", "some fallback", empty())), any());
    }

    @Test
    public void fetchesContentWithTtl() {
        final IncludedService service = new IncludedService.Builder()
            .attribute("ttl", "123")
            .build();

        service.fetch(fetcher, root("/"));

        verify(fetcher).fetch(eq(FetchContext.of("", null, Optional.of(Duration.ofMillis(123)))), any());
    }

    @Test
    public void usesDefaultTtlIfTtlNotParseable() {
        final Optional<Duration> expectedTtl = empty();
        final IncludedService service = new IncludedService.Builder()
            .attribute("ttl", "xyz")
            .build();

        service.fetch(fetcher, root("/"));

        verify(fetcher).fetch(eq(FetchContext.of("", null, expectedTtl)), any());
    }

    @Test
    public void incrementsCompositionStepDuringFetch() {
        final IncludedService service = new IncludedService.Builder()
            .attribute("path", "/child")
            .build();

        service.fetch(fetcher, root("/"));

        verify(fetcher).fetch(any(), eq(root("/").childWith("/child")));
    }

    @Test
    public void propagatesResponseToComposer() {
        final Response<String> response = Response.of(Status.OK, "");
        when(fetcher.fetch(any(), any())).thenReturn(CompletableFuture.completedFuture(response));

        new IncludedService.Builder()
            .attribute("path", "child/")
            .build()
            .fetch(fetcher, root("/"))
            .thenAccept(resp -> resp.compose(composer));

        verify(composer).composeContent(eq(response), eq(root("/").childWith("child/")));
    }

    @Test
    public void limitesCompositionToContentRange() throws Exception {
        when(fetcher.fetch(any(), any())).thenReturn(CompletableFuture.completedFuture(Response.of(Status.OK, "")));
        when(composer.composeContent(any(), any())).thenReturn(CompletableFuture.completedFuture(composition));

        final int startOffset = 0;
        final int endOffset = 100;

        new IncludedService.Builder()
            .attribute("path", "child/")
            .startOffset(startOffset)
            .endOffset(endOffset)
            .build()
            .fetch(fetcher, root("/"))
            .thenCompose(resp -> resp.compose(composer));

        verify(composition).forRange(startOffset, endOffset);
    }
}
