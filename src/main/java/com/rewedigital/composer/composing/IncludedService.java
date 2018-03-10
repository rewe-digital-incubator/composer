package com.rewedigital.composer.composing;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rewedigital.composer.composing.fetch.ContentFetcher;
import com.rewedigital.composer.composing.fetch.FetchContext;
import com.spotify.apollo.Response;

/**
 * Describes the include parsed from a template. It contains the start and end offsets of the include element in the
 * template for further processing in a {@link Composition}.
 *
 * An included service can {@link #fetch(ContentFetcher, CompositionStep)} the content using a {@link ContentFetcher}
 * creating an instance of {@link IncludedService.WithResponse} that holds the response.
 */
class IncludedService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IncludedService.class);

    public static class Builder {
        private int startOffset;
        private int endOffset;
        private final Map<String, String> attributes = new HashMap<>();
        private String fallback;

        public Builder startOffset(final int startOffset) {
            this.startOffset = startOffset;
            return this;
        }

        public Builder endOffset(final int endOffset) {
            this.endOffset = endOffset;
            return this;
        }

        public Builder attribute(final String name, final String value) {
            attributes.put(name, value);
            return this;
        }

        public Builder fallback(final String fallback) {
            this.fallback = fallback;
            return this;
        }

        public IncludedService build() {
            return new IncludedService(this);
        }
    }

    public static class WithResponse {
        private final int startOffset;
        private final int endOffset;
        private final Response<String> response;
        private final CompositionStep step;

        private WithResponse(final CompositionStep step, final int startOffset, final int endOffset,
            final Response<String> response) {
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.response = response;
            this.step = step;

            LOGGER.debug("included service response: {} received via {}", response, step);
        }

        public CompletableFuture<Composition> compose(final ContentComposer contentComposer) {
            return contentComposer
                .composeContent(response, step)
                .thenApply(c -> c.forRange(startOffset, endOffset));
        }
    }

    private final int startOffset;
    private final int endOffset;
    private final Map<String, String> attributes;
    private final String fallback;

    private IncludedService(final Builder builder) {
        this.startOffset = builder.startOffset;
        this.endOffset = builder.endOffset;
        this.attributes = new HashMap<>(builder.attributes);
        this.fallback = builder.fallback;
    }


    public CompletableFuture<IncludedService.WithResponse> fetch(final ContentFetcher fetcher,
        final CompositionStep parentStep) {
        final CompositionStep step = parentStep.childWith(path());
        return fetcher.fetch(FetchContext.of(path(), fallback(), ttl()), step)
            .thenApply(r -> new WithResponse(step, startOffset, endOffset, r));
    }

    private String fallback() {
        return fallback;
    }

    private String path() {
        return attributes.getOrDefault("path", "");
    }

    private Duration ttl() {
        // FIXME: Initialise with global default...
        final Duration defaultDuration = Duration.ofMillis(Long.MAX_VALUE);

        if (!attributes.containsKey("ttl")) {
            return defaultDuration;
        }

        // FIMXE:
        final String unparsedTtl = attributes.get("ttl");
        long ttl = Long.MAX_VALUE;
        try {
            ttl = Long.parseLong(unparsedTtl);
        } catch (final NumberFormatException nfEx) {
            LOGGER.info(
                "Not able to evaluate ttl for path {} with value {} falling back to the default of {}ms",
                path(), unparsedTtl, defaultDuration);
        }

        return Duration.ofMillis(ttl);
    }

    public boolean isInRage(final ContentRange contentRange) {
        return contentRange.isInRange(startOffset);
    }

}
