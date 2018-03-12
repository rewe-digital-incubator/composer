package com.rewedigital.composer.routing;

import static com.rewedigital.composer.routing.RouteTypeName.PROXY;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import com.rewedigital.composer.configuration.DefaultConfiguration;
import com.rewedigital.composer.routing.Match;
import com.rewedigital.composer.routing.RouteTypeName;
import com.rewedigital.composer.routing.RoutingConfiguration;
import com.spotify.apollo.route.Rule;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class RoutingConfigurationTest {

    private final int timeoutInMs = 200;
    private final Optional<Integer> withoutTtl = Optional.empty();
    private final Optional<Integer> withTtl = Optional.of(timeoutInMs);
    private final Duration ttlDuration = Duration.ofMillis(timeoutInMs);

    @Test
    public void allConfigParametersAreCoveredByDefaultConfig() {
        final RoutingConfiguration configuration =
            RoutingConfiguration.fromConfig(DefaultConfiguration.defaultConfiguration().getConfig("composer.routing"));
        assertThat(configuration.localRules()).isEmpty();
    }

    @Test
    public void createsLocalRouteFromConfiguration() {
        final RoutingConfiguration configuration =
            RoutingConfiguration.fromConfig(configWithSingleLocalRoute(withoutTtl));
        final List<Rule<Match>> localRules = configuration.localRules();
        assertThat(localRules).anySatisfy(rule -> {
            assertThat(rule.getPath()).isEqualTo("/test/path/<arg>");
            assertThat(rule.getMethods()).contains("GET");
            final Match routeMatchWithoutTtl =
                Match.of("https://target.service/{arg}", PROXY);
            assertThat(rule.getTarget()).isEqualTo(routeMatchWithoutTtl);
        });
    }

    @Test
    public void setsTtlIfConfigured() {
        final RoutingConfiguration configuration =
            RoutingConfiguration.fromConfig(configWithSingleLocalRoute(withTtl));
        final List<Rule<Match>> localRules = configuration.localRules();
        assertThat(localRules).anySatisfy(rule -> {
            final Match routeMatchWithTtl = Match.of("https://target.service/{arg}", ttlDuration, PROXY);
            assertThat(rule.getTarget()).isEqualTo(routeMatchWithTtl);
        });
    }

    private Config configWithSingleLocalRoute(final Optional<Integer> ttl) {
        return configWithLocalRoutes(
            localRoute("/test/path/<arg>", "GET", "https://target.service/{arg}", ttl, PROXY));
    }


    private static Map<String, Object> localRoute(final String path, final String method, final String target,
        final Optional<Integer> ttl, final RouteTypeName type) {
        final Map<String, Object> result = new HashMap<>();
        result.put("path", path);
        result.put("method", method);
        result.put("target", target);
        ttl.ifPresent(t -> result.put("ttl", t));
        result.put("type", type.toString());
        return result;
    }

    @SafeVarargs
    private static Config configWithLocalRoutes(final Map<String, Object>... routes) {
        final List<Map<String, Object>> routesConfigList = Arrays.asList(routes);
        final Map<String, Object> routesConfig = new HashMap<>();
        routesConfig.put("composer.routing.local-routes", routesConfigList);
        return DefaultConfiguration
            .withDefaults(ConfigFactory.parseMap(routesConfig, "test-routes-configuration"))
            .getConfig("composer.routing");
    }
}
