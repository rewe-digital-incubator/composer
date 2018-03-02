package com.rewedigital.composer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import org.junit.Rule;
import org.junit.Test;

import com.spotify.apollo.Response;
import com.spotify.apollo.test.ServiceHelper;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import okio.ByteString;

public class CompositionTest {

    @Rule
    public final MockWebServer server = new MockWebServer();

    @Rule
    public ServiceHelper serviceHelper =
        ServiceHelper.create(ComposerApplication.Initializer::init, ComposerApplication.COMPOSER)
            .conf("composer.routing.local-routes", routesConfig(mockServerUrl()))
            .conf("composer.html.include-tag", "include")
            .conf("composer.html.content-tag", "content");

    @Test
    public void parsesMasterTemplateAndCombinesChildTemplate() throws Exception {
        mockMicroServices();

        final CompletionStage<Response<ByteString>> composedFuture = serviceHelper.request("GET", "/compose");
        assertThat(responseBody(composedFuture)).isEqualTo("4223");
    }

    private String responseBody(final CompletionStage<Response<ByteString>> composedFuture)
        throws InterruptedException, ExecutionException {
        return composedFuture.toCompletableFuture().get().payload().get().utf8();
    }

    private void mockMicroServices() {
        server.enqueue(
            new MockResponse().setBody("42<include path=\"" + mockServerUrl() + "\">Fallback</include>")
        );
        
        server.enqueue(
            new MockResponse().setBody("<content>23</content>").setHeader("Content-Type", "text/html")
        );
    }

    private String mockServerUrl() {
        return server.url("/").toString();
    }

    private List<Map<String, Object>> routesConfig(final String templateService) {
        final Map<String, Object> route = new HashMap<>();
        route.put("path", "/compose");
        route.put("method", "GET");
        route.put("type", "TEMPLATE");
        route.put("target", templateService);
        return Collections.singletonList(route);
    }

}
