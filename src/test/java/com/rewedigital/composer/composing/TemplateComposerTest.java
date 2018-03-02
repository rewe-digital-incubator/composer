package com.rewedigital.composer.composing;

import static java.util.Arrays.asList;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static okio.ByteString.encodeUtf8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import org.junit.Test;

import com.rewedigital.composer.session.SessionRoot;
import com.spotify.apollo.Client;
import com.spotify.apollo.Response;
import com.spotify.apollo.Status;

import okio.ByteString;

public class TemplateComposerTest {

    @Test
    public void IgnoresIncludeWhenPathIsMissing() throws Exception {
        final TemplateComposer composer = makeComposer(aClientWithSimpleContent("should not be included"));

        final Response<String> result = composer
            .composeTemplate(r("template <include></include> content"), "template-path")
            .get()
            .response();

        assertThat(result.payload()).contains("template  content");
    }

    @Test
    public void composesSimpleTemplate() throws Exception {
        final String content = "content";
        final TemplateComposer composer = makeComposer(aClientWithSimpleContent(content));

        final Response<String> result = composer
            .composeTemplate(r(
                "template content <include path=\"http://mock/\"></include> more content"),
                "template-path")
            .get().response();

        assertThat(result.payload()).contains("template content " + content + " more content");
    }

    @Test
    public void appendsCSSLinksToHead() throws Exception {
        final TemplateComposer composer = makeComposer(aClientWithSimpleContent("",
            "<link href=\"css/link\" data-rd-options=\"include\" rel=\"stylesheet\"/>"));
        final Response<String> result = composer
            .composeTemplate(r("<head></head><include path=\"http://mock/\"></include>"),
                "template-path")
            .get().response();
        assertThat(result.payload()).contains(
            "<head><link rel=\"stylesheet\" data-rd-options=\"include\" href=\"css/link\" />\n" +
                "</head>");
    }

    @Test
    public void composesRecursiveTemplate() throws Exception {
        final String innerContent = "some content";
        final TemplateComposer composer = makeComposer(
            aClientWithConsecutiveContent(
                "<include path=\"http://other/mock/\"></include>",
                innerContent));
        final Response<String> result = composer
            .composeTemplate(r("<include path=\"http://mock/\"></include>"), "template-path")
            .get().response();
        assertThat(result.payload()).contains(innerContent);
    }

    @Test
    public void usesFallbackIfContentReturnsWithError() throws Exception {
        final TemplateComposer composer = makeComposer(aClientReturning(Status.BAD_REQUEST));
        final Response<String> result = composer
            .composeTemplate(
                r("template content <include path=\"http://mock/\"><content>"
                    + "<div>default content</div></content></include>"),
                "template-path")
            .get().response();
        assertThat(result.payload()).contains("template content <div>default content</div>");
    }

    @Test
    public void composesSessionAlongWithTemplates() throws Exception {
        final TemplateComposer composer =
            makeComposer(aClientWithSimpleContent("content", "x-rd-session-key-content", "session-val-content"));

        final SessionRoot result = composer
            .composeTemplate(r(
                "template content <include path=\"http://mock/\"></include> more content")
                    .withHeader("x-rd-session-key-template", "session-val-template"),
                "template-path")
            .get().session();

        assertThat(result.get("session-key-template")).contains("session-val-template");
        assertThat(result.get("session-key-content")).contains("session-val-content");
        assertThat(result.isDirty()).isTrue();
    }

    @Test
    public void limitsRecursionDepth() throws Exception {
        final int maxRecursion = 1;
        final String fallbackContent = "fallback";
        final TemplateComposer composer = makeComposerWithMaxRecursion(
            aClientWithConsecutiveContent(
                "<include path=\"include-exceeding-max-recursion\"><content>"
                    + fallbackContent + "</content></include>",
                "inner content should not be present"),
            maxRecursion);
        final Response<String> result = composer
            .composeTemplate(r("<include path=\"include-in-template\"></include>"),
                "template-path")
            .get().response();
        assertThat(result.payload()).contains(fallbackContent);
    }

    private TemplateComposer makeComposer(final Client client) {
        return makeComposerWithMaxRecursion(client, 10);
    }

    private TemplateComposer makeComposerWithMaxRecursion(final Client client, final int maxRecursion) {
        final SessionRoot session = SessionRoot.empty();
        return new AttoParserBasedComposer(
            new ValidatingContentFetcher(client, Collections.emptyMap(), session), session,
            new ComposerHtmlConfiguration("include", "content", "data-rd-options",
                maxRecursion));
    }

    private Client aClientWithSimpleContent(final String content) {
        return aClientWithSimpleContent(content, "");
    }


    private Client aClientWithSimpleContent(final String content, final String sessionKey, final String sessionValue) {
        return aClientReturning(contentResponse(content, "").withHeader(sessionKey, sessionValue));
    }

    private Client aClientWithSimpleContent(final String content, final String head) {
        return aClientReturning(contentResponse(content, head));
    }

    private Client aClientReturning(final Response<ByteString> response) {
        final Client client = mock(Client.class);
        when(client.send(any()))
            .thenReturn(completedFuture(response));
        return client;
    }

    private Client aClientWithConsecutiveContent(final String firstContent, final String... other) {
        final Client client = mock(Client.class);
        @SuppressWarnings("unchecked")
        final CompletableFuture<Response<ByteString>>[] otherResponses = asList(other)
            .stream()
            .map(c -> completedFuture(contentResponse(c, "")))
            .collect(Collectors.toList()).toArray(new CompletableFuture[0]);

        when(client.send(any())).thenReturn(completedFuture(contentResponse(firstContent, "")),
            otherResponses);
        return client;
    }

    private Client aClientReturning(final Status status) {
        final Client client = mock(Client.class);
        when(client.send(any())).thenReturn(statusResponse(status));
        return client;
    }


    private Response<ByteString> contentResponse(final String content, final String head) {
        return Response
            .forPayload(
                encodeUtf8("<html><head>" + head + "</head><body><content>" + content
                    + "</content></body></html>"))
            .withHeader("Content-Type", "text/html");
    }

    private CompletionStage<Response<ByteString>> statusResponse(final Status status) {
        return completedFuture(Response.forStatus(status));
    }

    private Response<String> r(final String body) {
        return Response.forPayload(body);
    }
}
