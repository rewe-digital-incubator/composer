package com.rewedigital.composer.composing;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.spotify.apollo.Response;

public class RecursionAwareContentFetcher implements ContentFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecursionAwareContentFetcher.class);

    private final ContentFetcher contentFetcher;
    private final int maxRecursion;

    public RecursionAwareContentFetcher(final ContentFetcher contentFetcher, final int maxRecursion) {
        this.contentFetcher = requireNonNull(contentFetcher);
        this.maxRecursion = maxRecursion;
    }

    @Override
    public CompletableFuture<Response<String>> fetch(final FetchContext context, final CompositionStep step) {
        if (maxRecursion < step.depth()) {
            LOGGER.warn("Max recursion depth exceeded for " + step.callStack());
            return CompletableFuture.completedFuture(Response.forPayload(context.fallback()));
        }
        return contentFetcher.fetch(context, step);
    }

}
