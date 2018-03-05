package com.rewedigital.composer.caching;

import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.spotify.apollo.environment.ClientDecorator;
import com.spotify.apollo.module.AbstractApolloModule;

public class HttpCacheModule extends AbstractApolloModule {

    public static HttpCacheModule create() {
        return new HttpCacheModule();
    }

    @Override
    public String getId() {
        return "http-cache";
    }

    @Override
    protected void configure() {
        binder().bind(HttpCache.class).in(Singleton.class);
        Multibinder.newSetBinder(binder(), ClientDecorator.class).addBinding().to(CachingClientDecorator.class);
    }

}
