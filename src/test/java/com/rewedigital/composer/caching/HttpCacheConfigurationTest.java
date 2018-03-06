package com.rewedigital.composer.caching;

import org.junit.Test;

import com.rewedigital.composer.configuration.DefaultConfiguration;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigValueFactory;

public class HttpCacheConfigurationTest {

    @Test
    public void allConfigParametersAreCoveredByDefaultConfig() {
        HttpCacheConfiguration.fromConfig(DefaultConfiguration.defaultConfiguration().getConfig("composer.http.cache"));
    }

    @Test(expected = ConfigException.BadValue.class)
    public void failsIfCacheSizeIsNegative() {
        HttpCacheConfiguration.fromConfig(DefaultConfiguration.defaultConfiguration().getConfig("composer.http.cache")
            .withValue("size", ConfigValueFactory.fromAnyRef(-1)));
    }


}
