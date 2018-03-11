package com.rewedigital.composer.composing;

import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.util.Optional;

/**
 * A simple parameter object for {@link ContentFetcher}s.
 */
public class FetchContext {

    private final String path;
    private final String fallback;
    private final Optional<Duration> ttl;

    private FetchContext(final String path, final String fallback, final Optional<Duration> ttl) {
        this.path = path;
        this.fallback = fallback;
        this.ttl = requireNonNull(ttl);
    }

    /**
     * Builds a simple parameter object for {@link ContentFetcher}s.
     *
     * @param path to fetch from.
     * @param fallback the fallback returned in case of an error.
     * @param ttl how long the fetch should take.
     * @return the parameter object.
     */
    public static FetchContext of(final String path, final String fallback, final Optional<Duration> ttl) {
        return new FetchContext(path, fallback, ttl);
    }

    public String path() {
        return path;
    }

    public String fallback() {
        return fallback;
    }

    public Optional<Duration> ttl() {
        return ttl;
    }
}
