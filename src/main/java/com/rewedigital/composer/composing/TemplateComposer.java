package com.rewedigital.composer.composing;

import java.util.concurrent.CompletableFuture;

import com.rewedigital.composer.session.ResponseWithSession;
import com.spotify.apollo.Response;

/**
 * <code>TemplateComposer</code> provides the interface to execute the main purpose of <em>Composer</em>: Composing a
 * web page out of a template and content fragments.
 *
 */
public interface TemplateComposer {

    CompletableFuture<ResponseWithSession<String>> composeTemplate(Response<String> templateResponse,
        String templatePath);

}
