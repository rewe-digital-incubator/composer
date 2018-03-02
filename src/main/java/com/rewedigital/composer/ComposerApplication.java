package com.rewedigital.composer;

import static com.rewedigital.composer.configuration.DefaultConfiguration.withDefaults;

import com.rewedigital.composer.client.ClientDecoratingModule;
import com.rewedigital.composer.client.ErrorClientDecorator;
import com.rewedigital.composer.composing.ComposerFactory;
import com.rewedigital.composer.proxy.ComposingRequestHandler;
import com.rewedigital.composer.routing.BackendRouting;
import com.rewedigital.composer.routing.RouteTypes;
import com.rewedigital.composer.routing.SessionAwareProxyClient;
import com.rewedigital.composer.session.CookieBasedSessionHandler;
import com.spotify.apollo.Environment;
import com.spotify.apollo.core.Service;
import com.spotify.apollo.http.client.HttpClientModule;
import com.spotify.apollo.httpservice.HttpService;
import com.spotify.apollo.httpservice.LoadingException;
import com.spotify.apollo.route.Route;
import com.typesafe.config.Config;

public class ComposerApplication {

    static final String COMPOSER = "composer";

    public static void main(final String[] args) throws LoadingException {
        HttpService.boot(bootstrapService(), args);
    }

    private static Service bootstrapService() {
        return HttpService
            .usingAppInit(Initializer::init, COMPOSER)
            .withEnvVarPrefix(COMPOSER.toUpperCase())
            .withModule(HttpClientModule.create())
            .withModule(ClientDecoratingModule.create(new ErrorClientDecorator()))
            .build();
    }

    static class Initializer {

        static void init(final Environment environment) {
            final Config configuration = withDefaults(environment.config());

            final ComposingRequestHandler handler =
                new ComposingRequestHandler(
                    new BackendRouting(configuration.getConfig(COMPOSER + ".routing")),
                    new RouteTypes(
                        new ComposerFactory(configuration.getConfig(COMPOSER + ".html")),
                        new SessionAwareProxyClient()),
                    new CookieBasedSessionHandler.Factory(configuration.getConfig(COMPOSER + ".session")));

            configureRoutes(environment, handler);
        }

        private static void configureRoutes(final Environment environment, final ComposingRequestHandler handler) {
            environment.routingEngine()
                .registerAutoRoute(Route.async("GET", "/", rc -> handler.execute(rc)))
                .registerAutoRoute(Route.async("HEAD", "/", rc -> handler.execute(rc)))
                .registerAutoRoute(Route.async("POST", "/", rc -> handler.execute(rc)))
                .registerAutoRoute(Route.async("PUT", "/", rc -> handler.execute(rc)))
                .registerAutoRoute(Route.async("DELETE", "/", rc -> handler.execute(rc)))
                .registerAutoRoute(Route.async("TRACE", "/", rc -> handler.execute(rc)))
                .registerAutoRoute(Route.async("OPTIONS", "/", rc -> handler.execute(rc)))
                .registerAutoRoute(Route.async("PATCH", "/", rc -> handler.execute(rc)))
                .registerAutoRoute(Route.async("GET", "/<path:path>", rc -> handler.execute(rc)))
                .registerAutoRoute(Route.async("HEAD", "/<path:path>", rc -> handler.execute(rc)))
                .registerAutoRoute(Route.async("POST", "/<path:path>", rc -> handler.execute(rc)))
                .registerAutoRoute(Route.async("PUT", "/<path:path>", rc -> handler.execute(rc)))
                .registerAutoRoute(Route.async("DELETE", "/<path:path>", rc -> handler.execute(rc)))
                .registerAutoRoute(Route.async("TRACE", "/<path:path>", rc -> handler.execute(rc)))
                .registerAutoRoute(Route.async("OPTIONS", "/<path:path>", rc -> handler.execute(rc)))
                .registerAutoRoute(Route.async("PATCH", "/<path:path>", rc -> handler.execute(rc)));
        }

    }

}
