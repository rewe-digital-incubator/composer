package com.rewedigital.composer.composing;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
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

    public boolean isInRage(final ContentRange contentRange) {
        return contentRange.isInRange(startOffset);
    }

    @VisibleForTesting
    public String fallback() {
        return fallback;
    }

    @VisibleForTesting
    public String path() {
        return attributes.getOrDefault("path", "");
    }

    @VisibleForTesting
    public Optional<Duration> ttl() {
        return longFromMap("ttl").map(Duration::ofMillis);
    }

    private Optional<Long> longFromMap(final String name) {
        if (!attributes.containsKey(name)) {
            return Optional.empty();
        }
        final String unparsedValue = attributes.get(name);
        try {
            return Optional.of(Long.parseLong(unparsedValue));
        } catch (final NumberFormatException nfEx) {
            LOGGER.info("Not able to evaluate {} for path {} with value {}.", name, path(), unparsedValue);
        }
        return Optional.empty();
    }

}
