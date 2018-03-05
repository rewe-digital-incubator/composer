package com.rewedigital.composer.client;

import com.google.inject.multibindings.Multibinder;
import com.spotify.apollo.environment.ClientDecorator;
import com.spotify.apollo.module.AbstractApolloModule;
import com.spotify.apollo.module.ApolloModule;

public class ErrorHandlingClientDecoratingModule extends AbstractApolloModule {

    private ErrorHandlingClientDecoratingModule() {
    }

    public static ApolloModule create() {
        return new ErrorHandlingClientDecoratingModule();
    }

    @Override
    protected void configure() {
        Multibinder.newSetBinder(binder(), ClientDecorator.class).addBinding().toInstance(new ErrorHandlingClientDecorator());
    }

    @Override
    public String getId() {
        return "error-handling-client-decorator";
    }

}
