package com.rewedigital.composer.client;

import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.spotify.apollo.environment.ClientDecorator;
import com.spotify.apollo.module.AbstractApolloModule;

public class WithIncomingHeadersClientDecoratingModule extends AbstractApolloModule {

    public static WithIncomingHeadersClientDecoratingModule create() {
        return new WithIncomingHeadersClientDecoratingModule();
    }

    @Override
    public String getId() {
        return "incoming-headers";
    }

    @Override
    protected void configure() {
        binder().bind(WithIncomingHeadersClientDecorator.class).in(Singleton.class);
        Multibinder.newSetBinder(binder(), ClientDecorator.class).addBinding().to(WithIncomingHeadersClientDecorator.class);
    }

}
