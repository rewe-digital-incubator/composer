package com.rewedigital.composer.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.spotify.apollo.Response;
import com.spotify.apollo.Status;
import com.spotify.apollo.environment.ClientDecorator;
import com.spotify.apollo.environment.IncomingRequestAwareClient;

import okio.ByteString;

public class ErrorHandlingClientDecorator implements ClientDecorator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorHandlingClientDecorator.class);

    @Override
    public IncomingRequestAwareClient apply(final IncomingRequestAwareClient requestAwareClient) {
        return (request, incoming) -> {
            return requestAwareClient.send(request, incoming)
                .exceptionally(ex -> {
                    LOGGER.warn("got exceptional response from {}, mapping to {}", request,
                        Status.INTERNAL_SERVER_ERROR, ex);
                    return Response.of(Status.INTERNAL_SERVER_ERROR, ByteString.encodeUtf8(""));
                });
        };
    }

}
