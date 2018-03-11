package com.rewedigital.composer.composing;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.damnhandy.uri.template.UriTemplate;
import com.rewedigital.composer.session.SessionRoot;
import com.spotify.apollo.Client;
import com.spotify.apollo.Request;
import com.spotify.apollo.Response;

import okio.ByteString;

public class ValidatingContentFetcher implements ContentFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidatingContentFetcher.class);

    private final Client client;
    private final Map<String, Object> parsedPathArguments;
    private final SessionRoot session;

    public ValidatingContentFetcher(final Client client, final Map<String, Object> parsedPathArguments,
        final SessionRoot session) {
        this.session = requireNonNull(session);
        this.client = requireNonNull(client);
        this.parsedPathArguments = requireNonNull(parsedPathArguments);
    }

    @Override
    public CompletableFuture<Response<String>> fetch(final FetchContext context, final CompositionStep step) {
        if (context.path() == null || context.path().trim().isEmpty()) {
            LOGGER.warn("Empty path attribute in include found - callstack: " + step.callStack());
            return CompletableFuture.completedFuture(Response.forPayload(""));
        }

        final String expandedPath = UriTemplate.fromTemplate(context.path()).expand(parsedPathArguments);
        final Request request = session.enrich(withTtl(Request.forUri(expandedPath, "GET"), context));


        return client.send(request)
            .thenApply(response -> acceptHtmlOnly(response, expandedPath))
            .thenApply(r -> toStringPayload(r, context.fallback()))
            .toCompletableFuture();
    }

    private Request withTtl(final Request request, final FetchContext context) {
        if (context.ttl().isPresent()) {
            return request.withTtl(context.ttl().get());
        }
        return request;
    }

    private Response<String> toStringPayload(final Response<ByteString> response, final String fallback) {
        final String value = response.payload().map(p -> p.utf8()).orElse(fallback);
        return response.withPayload(value);
    }

    private Response<ByteString> acceptHtmlOnly(final Response<ByteString> response, final String expandedPath) {
        final String contentType = response.header("Content-Type").orElse("other");
        if (contentType != null && contentType.contains("text/html")) {
            return response;
        }
        LOGGER.warn("Content-Type of [{}] is not text/html, returning an empty body.", expandedPath);
        return response.withPayload(null);
    }
}
