package com.rewedigital.composer.composing;

import org.junit.Test;

import com.rewedigital.composer.configuration.DefaultConfiguration;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigValueFactory;

public class ComposerHtmlConfigurationTest {

    @Test
    public void readsDefaultConfiguration() {
        ComposerHtmlConfiguration
            .fromConfig(DefaultConfiguration.defaultConfiguration().getConfig("composer.html"));
    }

    @Test(expected = ConfigException.BadValue.class)
    public void validatesMaxRecursionConfiguration() {
        ComposerHtmlConfiguration
            .fromConfig(DefaultConfiguration.defaultConfiguration().getConfig("composer.html")
                .withValue("max-recursion", ConfigValueFactory.fromAnyRef(-1)));
    }

}
