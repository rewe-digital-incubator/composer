package com.rewedigital.composer.routing;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import com.rewedigital.composer.session.ResponseWithSession;
import com.rewedigital.composer.session.SessionFragment;
import com.rewedigital.composer.session.SessionRoot;
import com.spotify.apollo.Request;
import com.spotify.apollo.RequestContext;

import okio.ByteString;

public class SessionAwareProxyClient {

    public CompletionStage<ResponseWithSession<ByteString>> fetch(final RouteMatch rm, final RequestContext context,
        final SessionRoot session) {
        return context.requestScopedClient()
            .send(session.enrich(withTtl(Request.forUri(rm.expandedPath(), context.request().method()), rm.ttl())))
            .thenApply(r -> new ResponseWithSession<>(r, session.mergedWith(SessionFragment.of(r))));
    }

    private Request withTtl(final Request request, final Optional<Duration> ttl) {
        return ttl.map(t -> request.withTtl(t)).orElse(request);
    }

}
