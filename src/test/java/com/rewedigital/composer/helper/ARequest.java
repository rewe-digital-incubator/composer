package com.rewedigital.composer.helper;

import java.time.Duration;
import java.util.Optional;

import org.mockito.ArgumentMatcher;

import com.spotify.apollo.Request;

public class ARequest {

    public static ArgumentMatcher<Request> withoutHeader(final String... headerNames) {
        return item -> {
            for (final String headerName : headerNames) {
                if (item.header(headerName).isPresent()) {
                    return false;
                }
            }
            return true;
        };
    }

    public static ArgumentMatcher<Request> withHeader(final String name, final String value) {
        return item -> item.header(name).equals(Optional.ofNullable(value));
    }

    public static ArgumentMatcher<Request> with(final String method, final String url, final String payload) {
        return request -> url.equals(request.uri()) && method.equals(request.method())
            && request.payload().isPresent() && payload.equals(request.payload().get().utf8());
    }

    public static ArgumentMatcher<Request> matching(final String path, final Optional<Long> ttl) {
        return item -> path.equals(item.uri()) && ttl.equals(item.ttl().map(Duration::toMillis));
    }

    public static ArgumentMatcher<Request> matching(final String path, final long ttl) {
        return matching(path, Optional.of(ttl));
    }
}
