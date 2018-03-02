package com.rewedigital.composer.composing;

import java.util.concurrent.CompletableFuture;

import com.rewedigital.composer.session.ResponseWithSession;
import com.spotify.apollo.Response;


public interface TemplateComposer {

    public CompletableFuture<ResponseWithSession<String>> composeTemplate(final Response<String> templateResponse,
        final String path);

}
