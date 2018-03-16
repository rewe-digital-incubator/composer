package com.rewedigital.composer.proxy;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.function.BinaryOperator;

import com.spotify.apollo.Request;
import com.spotify.apollo.RequestContext;
import com.spotify.apollo.Response;
import com.spotify.apollo.request.RequestContexts;
import com.spotify.apollo.route.AsyncHandler;

public class ProxyHeaderMiddleware {

    public static final Collection<String> hopByHopHeaders =
        new HashSet<>(Arrays.asList("connection", "keep-alive", "proxy-authenticate",
            "proxy-authorization", "te", "trailer", "transfer-encoding", "upgrade"));

    public static <T> AsyncHandler<Response<T>> apply(final AsyncHandler<Response<T>> inner) {
        return requestContext -> {
            final RequestContext decorated = decoradeContext(requestContext);
            return inner.invoke(decorated);
        };
    }

    private static RequestContext decoradeContext(final RequestContext requestContext) {
        final Request originalRequest = requestContext.request();
        final Request decoratedRequest = originalRequest
            .headerEntries()
            .stream()
            .filter(ProxyHeaderMiddleware::isEndToEnd)
            .reduce(originalRequest.clearHeaders(),
                (r, e) -> r.withHeader(e.getKey(), e.getValue()), throwingCombiner())
            .withHeader("x-forwarded-path", originalRequest.uri());

        return RequestContexts.create(decoratedRequest, requestContext.requestScopedClient(),
            requestContext.pathArgs(), requestContext.metadata().arrivalTime().getNano(), requestContext.metadata());
    }

    private static boolean isEndToEnd(final Map.Entry<String, String> header) {
        return !hopByHopHeaders.contains(header.getKey().toLowerCase());
    }

    private static BinaryOperator<Request> throwingCombiner() {
        return (a, b) -> {
            throw new UnsupportedOperationException("Must not use parallel stream.");
        };
    }

}
