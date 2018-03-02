package com.rewedigital.composer.composing;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;

public class ComposerHtmlConfiguration {

    private final String includeTag;
    private final String contentTag;
    private final String assetOptionsAttribute;
    private final int maxRecursion;

    public static ComposerHtmlConfiguration fromConfig(final Config config) {
        final int maxRecursion = config.getInt("max-recursion");
        if (maxRecursion < 0) {
            throw new ConfigException.BadValue("max-recursion", "must be positive");
        }

        return new ComposerHtmlConfiguration(config.getString("include-tag"), config.getString("content-tag"),
            config.getString("asset-options-attribute"), maxRecursion);
    }

    ComposerHtmlConfiguration(final String includeTag, final String contentTag, final String assetOptionsAttribute,
        final int maxRecursion) {
        this.includeTag = includeTag;
        this.contentTag = contentTag;
        this.assetOptionsAttribute = assetOptionsAttribute;
        this.maxRecursion = maxRecursion;
    }

    public String includeTag() {
        return includeTag;
    }

    public String contentTag() {
        return contentTag;
    }

    public String assetOptionsAttribute() {
        return assetOptionsAttribute;
    }

    public int maxRecursion() {
        return maxRecursion;
    }
}
