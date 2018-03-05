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
import com.spotify.apollo.Status;
import com.spotify.apollo.StatusType;
import com.spotify.apollo.test.ServiceHelper;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import com.squareup.okhttp.mockwebserver.SocketPolicy;

import okio.ByteString;

public class CompositionTest {

    @Rule
    public final MockWebServer server = new MockWebServer();

    @Rule
    public ServiceHelper serviceHelper = ComposerApplication.bootstrap(ServiceHelper::create, (s, m) -> s.withModule(m))
        .conf("composer.routing.local-routes", routesConfig(mockServerUrl()))
        .conf("composer.html.include-tag", "include")
        .conf("composer.html.content-tag", "content")
        .conf("http.client.readTimeout", 500);

    @Test
    public void parsesMasterTemplateAndCombinesWithChildTemplate() throws Exception {
        mockUpstreamServices(templateResponse(), contentResponse());

        final CompletionStage<Response<ByteString>> composedFuture = serviceHelper.request("GET", "/compose");
        assertThat(responseBody(composedFuture)).isEqualTo(expectedComposedResponse());
    }

    @Test
    public void handlesErrorInTemplateRequestByMappingToErrorResponse() throws Exception {
        mockUpstreamServices(noResponse());

        final CompletionStage<Response<ByteString>> composedFuture = serviceHelper.request("GET", "/compose");
        assertThat(status(composedFuture)).isEqualTo(Status.INTERNAL_SERVER_ERROR);

    }

    private String expectedComposedResponse() {
        return "template+content";
    }

    private String responseBody(final CompletionStage<Response<ByteString>> composedFuture)
        throws InterruptedException, ExecutionException {
        return composedFuture.toCompletableFuture().get().payload().get().utf8();
    }

    private StatusType status(final CompletionStage<Response<ByteString>> composedFuture)
        throws InterruptedException, ExecutionException {
        return composedFuture.toCompletableFuture().get().status();
    }

    private void mockUpstreamServices(final MockResponse... mockResponses) {
        for (final MockResponse response : mockResponses) {
            server.enqueue(response);
        }
    }

    private MockResponse contentResponse() {
        return new MockResponse().setBody("<content>content</content>").setHeader("Content-Type", "text/html");
    }

    private MockResponse templateResponse() {
        return new MockResponse().setBody("template+<include path=\"" + mockServerUrl() + "\"></include>");
    }

    private MockResponse noResponse() {
        return new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE);
    }

    private String mockServerUrl() {
        return server.url("/").toString();
    }

    private static List<Map<String, Object>> routesConfig(final String templateService) {
        final Map<String, Object> route = new HashMap<>();
        route.put("path", "/compose");
        route.put("method", "GET");
        route.put("type", "TEMPLATE");
        route.put("target", templateService);
        return Collections.singletonList(route);
    }

}
