package com.rewedigital.composer.composing;

import java.util.concurrent.CompletableFuture;

import com.spotify.apollo.Response;

interface ContentComposer {
    CompletableFuture<Composition> composeContent(final Response<String> templateResponse, final CompositionStep step);
}
