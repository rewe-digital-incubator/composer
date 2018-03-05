package com.rewedigital.composer.caching;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.spotify.apollo.environment.ClientDecorator;
import com.spotify.apollo.environment.IncomingRequestAwareClient;

public class CachingClientDecorator implements ClientDecorator {

    private final Provider<HttpCache> cache;

    @Inject
    public CachingClientDecorator(final Provider<HttpCache> cache) {
        this.cache = cache;
    }

    @Override
    public IncomingRequestAwareClient apply(final IncomingRequestAwareClient client) {
        return (request, incoming) -> {
            return this.cache.get().withCaching(request, incoming, client);
        };
    }

}
