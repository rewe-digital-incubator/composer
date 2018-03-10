package com.rewedigital.composer.routing;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.spotify.apollo.route.Rule;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigList;
import com.typesafe.config.ConfigValue;

public class RoutingConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoutingConfiguration.class);
    private final List<Rule<Match>> localRoutes;

    public RoutingConfiguration(final List<Rule<Match>> localRoutes) {
        this.localRoutes = localRoutes;
    }

    public static RoutingConfiguration fromConfig(final Config config) {
        return new RoutingConfiguration(buildLocalRoutes(config.getList("local-routes")));
    }

    private static List<Rule<Match>> buildLocalRoutes(final ConfigList routesConfig) {
        return routesConfig.stream().map(RoutingConfiguration::buildLocalRoute).collect(Collectors.toList());
    }

    private static Rule<Match> buildLocalRoute(final ConfigValue configValue) {
        final Config config = configValue.atKey("route").getConfig("route");
        final String path = config.getString("path");
        final String method = config.getString("method");
        final String type = config.getString("type");
        final String target = config.getString("target");

        // FIXME: Don't error out if none is configured use a sane default.
        final Duration ttl = config.getDuration("ttl");

        final Rule<Match> result = Rule.fromUri(path, method, Match.of(target, ttl, RouteTypeName.valueOf(type)));
        LOGGER.info("Registered local route for path={}, method={}, target={}, type={} with a request ttl={}", path,
            method,
            target, type, ttl);
        return result;

    }

    public List<Rule<Match>> localRules() {
        return localRoutes;
    }

}
