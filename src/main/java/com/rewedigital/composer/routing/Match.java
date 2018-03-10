package com.rewedigital.composer.routing;

import java.time.Duration;
import java.util.Objects;

public class Match {

    private final String backend;
    private final Duration ttl;
    private final RouteTypeName routeType;

    private Match(final String backend, final Duration ttl, final RouteTypeName routeType) {
        this.backend = backend;
        this.ttl = ttl;
        this.routeType = routeType;
    }

    public static Match of(final String backend, final Duration ttl, final RouteTypeName routeType) {
        return new Match(backend, ttl, routeType);
    }

    public String backend() {
        return backend;
    }

    public Duration ttl() {
        return ttl;
    }

    public RouteType routeType(final RouteTypes routeTypes) {
        return routeType.from(routeTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(backend, ttl, routeType);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Match other = (Match) obj;
        return Objects.equals(backend, other.backend) && routeType == other.routeType && ttl == other.ttl;
    }

}
