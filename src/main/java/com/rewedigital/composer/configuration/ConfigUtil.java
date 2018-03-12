package com.rewedigital.composer.configuration;

import java.time.Duration;
import java.util.Optional;

import com.typesafe.config.Config;

/**
 * Utility class inspired by {@link com.spotify.apollo.environment.ConfigUtil} eases working with {@link Config}.
 */
public class ConfigUtil {

    private ConfigUtil() {
        // Construction is not permitted.
    }

    public static Optional<Duration> optionalDuration(final Config config, final String path) {
        return config.hasPath(path) ? Optional.of(config.getDuration(path)) : Optional.empty();
    }

}
