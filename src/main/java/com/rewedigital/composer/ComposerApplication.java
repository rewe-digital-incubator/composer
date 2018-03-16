package com.rewedigital.composer;

import static com.rewedigital.composer.configuration.DefaultConfiguration.withDefaults;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import com.rewedigital.composer.caching.HttpCacheModule;
import com.rewedigital.composer.client.ErrorHandlingClientDecoratingModule;
import com.rewedigital.composer.client.WithIncomingHeadersClientDecoratingModule;
import com.rewedigital.composer.composing.ComposerFactory;
import com.rewedigital.composer.proxy.ComposingRequestHandler;
import com.rewedigital.composer.proxy.ProxyHeaderMiddleware;
import com.rewedigital.composer.routing.BackendRouting;
import com.rewedigital.composer.routing.RouteTypes;
import com.rewedigital.composer.routing.SessionAwareProxyClient;
import com.rewedigital.composer.session.CookieBasedSessionHandler;
import com.spotify.apollo.AppInit;
import com.spotify.apollo.Environment;
import com.spotify.apollo.core.Service;
import com.spotify.apollo.httpservice.HttpService;
import com.spotify.apollo.httpservice.LoadingException;
import com.spotify.apollo.module.ApolloModule;
import com.spotify.apollo.route.Route;
import com.typesafe.config.Config;

public class ComposerApplication {

    private static final List<String> methods =
        Arrays.asList("GET", "HEAD", "POST", "PUT", "DELETE", "TRACE", "OPTIONS", "PATCH");

    public static void main(final String[] args) throws LoadingException {
        HttpService.boot(bootstrapService(), args);
    }

    private static Service bootstrapService() {
        return bootstrap(HttpService::usingAppInit, (b, m) -> b.withModule(m))
            .withEnvVarPrefix("COMPOSER")
            .build();
    }

    static <T> T bootstrap(final BiFunction<AppInit, String, T> init,
        final BiFunction<T, ApolloModule, T> moduleAppender) {
        T result = init.apply(Initializer::init, "composer");
        for (final ApolloModule module : additionalModules()) {
            result = moduleAppender.apply(result, module);
        }
        return result;
    }

    private static List<ApolloModule> additionalModules() {
        return Arrays.asList(
            WithIncomingHeadersClientDecoratingModule.create(), ErrorHandlingClientDecoratingModule.create(),
            HttpCacheModule.create());
    }

    private static class Initializer {

        static void init(final Environment environment) {
            final Config configuration = withDefaults(environment.config());

            final ComposingRequestHandler handler =
                new ComposingRequestHandler(
                    new BackendRouting(configuration.getConfig("composer.routing")),
                    new RouteTypes(
                        new ComposerFactory(configuration.getConfig("composer.html")),
                        new SessionAwareProxyClient()),
                    new CookieBasedSessionHandler.Factory(configuration.getConfig("composer.session")));

            registerRoutes(environment, handler, "/");
            registerRoutes(environment, handler, "/<path:path>");
        }

        private static void registerRoutes(final Environment environment, final ComposingRequestHandler handler,
            final String uri) {
            for (final String method : methods) {
                environment.routingEngine().registerAutoRoute(
                    Route.async(method, uri, handler::execute).withMiddleware(ProxyHeaderMiddleware::apply));
            }
        }
    }
}
