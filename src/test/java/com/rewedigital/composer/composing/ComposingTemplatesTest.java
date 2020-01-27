package com.rewedigital.composer.composing;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static okio.ByteString.encodeUtf8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.assertj.core.api.Condition;
import org.junit.Test;

import com.rewedigital.composer.application.ValidatingContentFetcher;
import com.rewedigital.composer.helper.ARequest;
import com.rewedigital.composer.html.ComposableBodyRoot;
import com.rewedigital.composer.session.SessionRoot;
import com.spotify.apollo.Client;
import com.spotify.apollo.Request;
import com.spotify.apollo.Response;
import com.spotify.apollo.Status;

import okio.ByteString;
import org.mockito.ArgumentMatcher;

public class ComposingTemplatesTest {

    @Test
    public void returns_just_the_template_if_it_does_not_contain_includes() throws Exception {
        when_composing_a_template("template content");
        then_the_result_should_be("template content");
    }

    @Test
    public void composes_content_from_downstream_call_into_template() throws Exception {
        given_first_downstream_call_returns_in_body("content");
        when_composing_a_template("template content <include path=\"http://mock/\"></include> more content");
        then_the_result_should_be("template content " + "content" + " more content");
    }

    @Test
    public void does_no_composition_if_include_path_is_missing() throws Exception {
        given_first_downstream_call_returns_in_body("should not be included");
        when_composing_a_template("template <include></include> content");
        then_the_result_should_be("template  content");
    }

    @Test
    public void appends_ccs_links_from_downstream_response_to_head() throws Exception {
        given_first_downstream_call_returns_in_head(
            "<link href=\"css/link\" data-rd-options=\"include\" rel=\"stylesheet\"/>");
        when_composing_a_template("<head></head><include path=\"http://mock/\"></include>");
        then_the_result_should_be("<head><link rel=\"stylesheet\" href=\"css/link\" />\n</head>");
    }

    @Test
    public void removes_ccs_link_not_marked_for_include_from_downstream_response() throws Exception {
        given_first_downstream_call_returns_in_head("<link href=\"css/link\" rel=\"stylesheet\"/>");
        when_composing_a_template("<head></head><include path=\"http://mock/\"></include>");
        then_the_result_should_not_contain("\"css/link\"");
    }

    @Test
    public void keeps_ordinary_ccs_link_in_template() throws Exception {
        when_composing_a_template("<head>" + "<link href=\"css/link\" rel=\"stylesheet\"/>" + "</head>");
        then_the_result_should_be("<head>" + "<link href=\"css/link\" rel=\"stylesheet\"/>" + "\n</head>");
    }

    @Test
    public void appends_js_links_from_downstream_response_to_head() throws Exception {
        given_first_downstream_call_returns_in_head(
            "<script src=\"js/link/script.js\" data-rd-options=\"include\" type=\"text/javascript\"></script>");
        when_composing_a_template("<head></head><include path=\"http://mock/\"></include>");
        then_the_result_should_be(
            "<head><script type=\"text/javascript\" src=\"js/link/script.js\" ></script>\n</head>");
    }

    @Test
    public void composes_content_from_recursive_downstream_calls_into_template() throws Exception {
        given_first_downstream_call_returns_in_body("first part <include path=\"http://other/mock/\"></include>");
        given_next_downstream_call_returns_in_body("second part");
        when_composing_a_template("<include path=\"http://mock/\"></include>");
        then_the_result_should_be("first part second part");
    }

    @Test
    public void uses_fallback_from_template_if_downstream_call_returns_with_error() throws Exception {
        given_first_downstream_call_returns_status(Status.BAD_REQUEST);
        when_composing_a_template("template content <include path=\"http://mock/\"><content>"
            + "<div>default content</div></content></include>");
        then_the_result_should_be("template content <div>default content</div>");
    }

    @Test
    public void uses_fallback_from_template_if_downstream_call_does_not_return_html() throws Exception {
        given_first_downstream_call_returns_content_type("text/json");
        when_composing_a_template("template content <include path=\"http://mock/\"><content>"
            + "<div>default content</div></content></include>");
        then_the_result_should_be("template content <div>default content</div>");
    }

    @Test
    public void limits_depth_of_recursive_downstream_calls() throws Exception {
        given_a_max_recursion_depth_of(1);
        given_first_downstream_call_returns_in_body(
            "<include path=\"include-exceeding-max-recursion\"><content>" + "fallback" + "</content></include>");
        given_next_downstream_call_returns_in_body("inner content should not be present");
        when_composing_a_template("<include path=\"include-in-template\"></include>");
        then_the_result_should_be("fallback");
    }

    @Test
    public void uses_ttl_from_include_if_present_to_determine_timeout() throws Exception {
        final int ttl = 500;
        when_composing_a_template(
            "template content <include ttl=\"" + ttl + "\" path=\"http://mock/\"></include> more content");
        then_the_downstream_call_should_be_made_with(ARequest.matching("http://mock/", ttl));
    }

    @Test
    public void can_handle_maleformatted_ttl_in_include_element() throws Exception {
        when_composing_a_template(
            "template content <include ttl=\"" + "abc" + "\" path=\"http://mock/\"></include> more content");
        then_the_downstream_call_should_be_made_with(ARequest.matching("http://mock/", Optional.empty()));
    }

    @Test
    public void composes_session_along_with_template_from_downstream_response_headers() throws Exception {
        given_first_downstream_call_returns_header("x-rd-session-key-content", "session-val-content");
        when_composing_a_template("template content <include path=\"http://mock/\"></include> more content",
            "x-rd-session-key-template", "session-val-template");
        then_the_session_should_contain("session-key-template", "session-val-template");
        then_the_session_should_contain("session-key-content", "session-val-content");
        then_the_session_should_be_dirty();
    }

    private Client client;
    private Composer newComposer;
    private Response<String> response;
    private SessionRoot session;
    private String downstreamBodyContent = "";
    private String downstreamHeadContent = "";
    private List<CompletableFuture<Response<ByteString>>> furtherDownstreamResponses = new LinkedList<>();
    private Map<String, String> downstreamHeaders = new HashMap<>();
    private Status downstreamStatus = Status.OK;
    private int maxRecursionDepth = 10;
    private String downstreamContentType = "text/html";

    private void given_first_downstream_call_returns_in_body(final String bodyContent) {
        downstreamBodyContent = bodyContent;
    }

    private void given_first_downstream_call_returns_in_head(final String headContent) {
        downstreamHeadContent = headContent;
    }

    private void given_next_downstream_call_returns_in_body(final String bodyContent) {
        furtherDownstreamResponses.add(completedFuture(contentResponse(Status.OK, bodyContent, "", "text/html")));
    }

    private void given_first_downstream_call_returns_status(final Status status) {
        downstreamStatus = status;
    }

    private void given_first_downstream_call_returns_header(final String key, final String value) {
        downstreamHeaders.put(key, value);
    }

    private void given_a_max_recursion_depth_of(final int recursionDepth) {
        maxRecursionDepth = recursionDepth;
    }

    private void given_first_downstream_call_returns_content_type(final String contentType) {
        downstreamContentType = contentType;
    }

    private void when_composing_a_template(final String template) throws Exception {
        final String body = template;
        when_composing_a_template(Response.forPayload(body));
    }

    private void when_composing_a_template(final String template, final String headerKey, final String headerValue)
        throws Exception {
        final String body = template;
        when_composing_a_template(Response.forPayload(body).withHeader(headerKey, headerValue));
    }

    private void when_composing_a_template(final Response<String> template) throws Exception {
        client = makeClient();
        newComposer = makeNewComposer(client, template);
        final ComposingResponse<String> result = newComposer.composeTemplate().get();
        response = result.composedResponse();
        session = result.getComposition(SessionRoot.class).get();
    }

    private void then_the_result_should_be(final String bodyContent) {
        assertThat(response.payload()).contains(bodyContent);
    }

    private void then_the_result_should_not_contain(final String value) {
        assertThat(response.payload()).doesNotHave(
            new Condition<>(v -> v.isPresent() && v.get().contains(value), "value containing %s", value));
    }

    private void then_the_downstream_call_should_be_made_with(final ArgumentMatcher<Request> matcher) {
        verify(client).send(argThat(matcher));
    }

    private void then_the_session_should_contain(final String sessionKey, final String sessionValue) {
        assertThat(session.get(sessionKey)).contains(sessionValue);
    }

    private void then_the_session_should_be_dirty() {
        assertThat(session.isDirty()).isTrue();
    }

    private Composer makeNewComposer(final Client client, final Response<String> template) {
        final ComposerHtmlConfiguration configuration = new ComposerHtmlConfiguration("include", "content",
            "data-rd-options", maxRecursionDepth);
        final ResponseComposition responseComposition = ResponseComposition
            .of(Arrays.asList(ComposableBodyRoot.of(configuration), SessionRoot.empty()));
        final ComposingResponse<String> composingResponse = ComposingResponse.of(template, responseComposition);
        final ContentFetcher contentFetcher = new ValidatingContentFetcher(client, Collections.emptyMap(),
            responseComposition,
            configuration.maxRecursion());
        return new Composer(contentFetcher, composingResponse, "template-path");
    }

    private Client makeClient() {
        final CompletableFuture<Response<ByteString>> firstResponse = completedFuture(
            contentResponse(downstreamStatus, downstreamBodyContent, downstreamHeadContent, downstreamContentType));
        @SuppressWarnings({"unchecked", "rawtypes"})
        final CompletableFuture<Response<ByteString>>[] otherResponses = furtherDownstreamResponses.toArray(new CompletableFuture[0]);

        final Client client = mock(Client.class);
        when(client.send(any())).thenReturn(firstResponse, otherResponses);
        return client;
    }

    private Response<ByteString> contentResponse(final Status status, final String content, final String head,
        final String contentType) {
        return Response.forStatus(status)
            .withPayload(encodeUtf8(
                "<html><head>" + head + "</head><body><content>" + content + "</content></body></html>"))
            .withHeader("Content-Type", contentType).withHeaders(downstreamHeaders);
    }
}
