package com.rewedigital.composer.routing;

import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import com.damnhandy.uri.template.UriTemplate;

public class RouteMatch {

    private final Match backend;
    private final Map<String, Object> parsedPathArguments;

    public RouteMatch(final Match backend, final Map<String, String> parsedPathArguments) {
        this.backend = requireNonNull(backend);
        this.parsedPathArguments = Collections.<String, Object>unmodifiableMap(parsedPathArguments);
    }

    public String backend() {
        return backend.backend();
    }

    public Optional<Duration> ttl() {
        return backend.ttl();
    }

    public RouteType routeType(final RouteTypes routeTypes) {
        return backend.routeType(routeTypes);
    }

    public Map<String, Object> parsedPathArguments() {
        return parsedPathArguments;
    }

    public String expandedPath() {
        return UriTemplate.fromTemplate(backend.backend()).expand(parsedPathArguments);
    }

    @Override
    public String toString() {
        return "RouteMatch [backend=" + backend + ", parsedPathArguments=" + parsedPathArguments + "]";
    }

}
