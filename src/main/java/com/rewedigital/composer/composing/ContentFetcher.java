package com.rewedigital.composer.composing;

import java.util.concurrent.CompletableFuture;

import com.spotify.apollo.Response;

/**
 * Fetches content for some include using a provided fallback in case of an error.
 */
public interface ContentFetcher {

    CompletableFuture<Response<String>> fetch(FetchContext fetchContext, CompositionStep step);

}
