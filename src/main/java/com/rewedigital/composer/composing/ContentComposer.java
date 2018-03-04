package com.rewedigital.composer.composing;

import java.util.concurrent.CompletableFuture;

import com.spotify.apollo.Response;

/**
 * Content composer provides the interface to execute the composition of a content fragment.
 */
interface ContentComposer {

    CompletableFuture<Composition> composeContent(Response<String> contentResponse, CompositionStep step);

}
