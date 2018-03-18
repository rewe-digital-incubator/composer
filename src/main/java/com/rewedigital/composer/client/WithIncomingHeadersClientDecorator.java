package com.rewedigital.composer.client;

import static com.rewedigital.composer.util.Combiners.throwingCombiner;

import java.util.Map;
import java.util.Optional;

import com.rewedigital.composer.session.SessionData;
import com.spotify.apollo.Request;
import com.spotify.apollo.environment.ClientDecorator;
import com.spotify.apollo.environment.IncomingRequestAwareClient;

public class WithIncomingHeadersClientDecorator implements ClientDecorator {

    @Override
    public IncomingRequestAwareClient apply(final IncomingRequestAwareClient client) {
        return (request, incoming) -> client.send(takeoverHeaders(request, incoming), incoming);
    }

    private Request takeoverHeaders(final Request request, final Optional<Request> incoming) {
        return incoming.map(r -> takeoverHeaders(request, r)).orElse(request);
    }

    private Request takeoverHeaders(final Request request, final Request incoming) {
        return incoming.headerEntries().stream()
            .filter(this::isNotSessionHeader)
            .reduce(request,
                (r, e) -> r.withHeader(e.getKey(), e.getValue()), throwingCombiner());
    }

    public boolean isNotSessionHeader(final Map.Entry<String, String> header) {
        return !SessionData.isSessionEntry(header);
    }
}