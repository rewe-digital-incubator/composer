package com.rewedigital.composer.caching;

import java.util.Map.Entry;

import com.spotify.apollo.Response;
import com.squareup.okhttp.CacheControl;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.Headers.Builder;

import okio.ByteString;

public class CacheHeaders {

    public static CacheControl of(final Response<ByteString> response) {
        Builder builder = new Headers.Builder();
        for (final Entry<String, String> entry : response.headerEntries()) {
            builder = builder.add(entry.getKey(), entry.getValue());
        }
        return CacheControl.parse(builder.build());
    }

}
