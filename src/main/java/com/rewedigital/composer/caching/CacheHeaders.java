package com.rewedigital.composer.caching;

import java.util.Map.Entry;

import com.spotify.apollo.Response;
import com.spotify.ffwd.http.okhttp3.CacheControl;
import com.spotify.ffwd.http.okhttp3.Headers;

import okio.ByteString;

public class CacheHeaders {

    public static CacheControl of(final Response<ByteString> response) {
        if (response == null) {
            new CacheControl.Builder().build();
        }

        Headers.Builder builder = new Headers.Builder();
        for (final Entry<String, String> entry : response.headerEntries()) {
            builder = builder.add(entry.getKey(), entry.getValue());
        }
        return CacheControl.parse(builder.build());
    }

}
