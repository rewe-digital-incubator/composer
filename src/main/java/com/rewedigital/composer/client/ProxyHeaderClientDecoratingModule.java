package com.rewedigital.composer.client;

import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.spotify.apollo.environment.ClientDecorator;
import com.spotify.apollo.module.AbstractApolloModule;

public class ProxyHeaderClientDecoratingModule extends AbstractApolloModule {

    public static ProxyHeaderClientDecoratingModule create() {
        return new ProxyHeaderClientDecoratingModule();
    }

    @Override
    public String getId() {
        return "proxy-headers";
    }

    @Override
    protected void configure() {
        binder().bind(ProxyHeaderClientDecorator.class).in(Singleton.class);
        Multibinder.newSetBinder(binder(), ClientDecorator.class).addBinding().to(ProxyHeaderClientDecorator.class);
    }

}
