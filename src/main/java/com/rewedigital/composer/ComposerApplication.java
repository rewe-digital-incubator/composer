package com.rewedigital.composer;

import static com.rewedigital.composer.application.DefaultConfiguration.withDefaults;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import com.rewedigital.composer.application.ProxyHeaderMiddleware;
import com.rewedigital.composer.application.RequestHandler;
import com.rewedigital.composer.application.RequestHandlerFactory;
import com.rewedigital.composer.caching.HttpCacheModule;
import com.rewedigital.composer.client.ErrorHandlingClientDecoratingModule;
import com.rewedigital.composer.client.WithIncomingHeadersClientDecoratingModule;
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
        int hallo = 23;
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

            final RequestHandler handler = RequestHandlerFactory.createRequestHandler(configuration);

            registerRoutes(environment, handler, "/");
            registerRoutes(environment, handler, "/<path:path>");
        }


        private static void registerRoutes(final Environment environment, final RequestHandler handler,
            final String uri) {
            for (final String method : methods) {
                environment.routingEngine().registerAutoRoute(
                    Route.async(method, uri, handler::execute).withMiddleware(ProxyHeaderMiddleware::apply));
            }
        }
    }
}
