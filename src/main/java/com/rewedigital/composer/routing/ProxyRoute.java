package com.rewedigital.composer.routing;

import java.util.Objects;
import java.util.concurrent.CompletionStage;

import com.rewedigital.composer.session.ResponseWithSession;
import com.rewedigital.composer.session.SessionRoot;
import com.spotify.apollo.RequestContext;

import okio.ByteString;

/**
 * {@link RouteType} <em>proxy</em> just proxies the request to the routing target.
 */
public class ProxyRoute implements RouteType {

    private final SessionAwareProxyClient templateClient;

    public ProxyRoute(final SessionAwareProxyClient templateClient) {
        this.templateClient = Objects.requireNonNull(templateClient);
    }

    @Override
    public CompletionStage<ResponseWithSession<ByteString>> execute(final RouteMatch rm, final RequestContext context,
        final SessionRoot session) {
        return templateClient.fetch(rm.expandedPath(), context, session);
    }
}
