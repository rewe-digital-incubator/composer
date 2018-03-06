package com.rewedigital.composer.caching;

import com.typesafe.config.Config;

public class HttpCacheConfiguration {

    private final boolean enabled;
    private final int size;

    public HttpCacheConfiguration(final boolean enabled, final int size) {
        this.enabled = enabled;
        this.size = size;
    }

    public static HttpCacheConfiguration fromConfig(final Config config) {
        final boolean enabled = config.getBoolean("enabled");
        final int size;
        if (enabled) {
            size = config.getInt("size");
        } else {
            size = 0;
        }
        return new HttpCacheConfiguration(enabled, size);
    }

    public boolean enabled() {
        return enabled;
    }

    public int size() {
        return size;
    }
}
