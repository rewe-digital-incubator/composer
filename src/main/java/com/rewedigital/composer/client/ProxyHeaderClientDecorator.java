package com.rewedigital.composer.client;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;

import com.spotify.apollo.Request;
import com.spotify.apollo.environment.ClientDecorator;
import com.spotify.apollo.environment.IncomingRequestAwareClient;

public class ProxyHeaderClientDecorator implements ClientDecorator {

    private static final Collection<String> hopByHopHeaders =
        new HashSet<>(Arrays.asList("connection", "keep-alive", "proxy-authenticate",
            "proxy-authorization", "te", "trailer", "transfer-encoding", "upgrade"));

    @Override
    public IncomingRequestAwareClient apply(final IncomingRequestAwareClient client) {
        return (request, incoming) -> client.send(takeoverHeaders(request, incoming), incoming);
    }

    private Request takeoverHeaders(final Request request, final Optional<Request> incoming) {
        return incoming.map(r -> takeoverHeaders(request, r)).orElse(request);
    }

    private Request takeoverHeaders(final Request request, final Request incoming) {
        return incoming.headerEntries().stream()
            .filter(this::isEndToEnd)
            .reduce(request,
                (r, e) -> r.withHeader(e.getKey(), e.getValue()), throwingCombiner())
            .withHeader("x-forwarded-path", incoming.uri());
    }

    private boolean isEndToEnd(final Map.Entry<String, String> header) {
        return !hopByHopHeaders.contains(header.getKey().toLowerCase());
    }

    private static BinaryOperator<Request> throwingCombiner() {
        return (a, b) -> {
            throw new UnsupportedOperationException("Must not use parallel stream.");
        };
    }
}
