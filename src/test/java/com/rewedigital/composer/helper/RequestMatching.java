package com.rewedigital.composer.helper;

import java.util.Optional;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import com.spotify.apollo.Request;

public class RequestMatching {

    public static Matcher<Request> withoutHeader(final String... headerNames) {
        return new TypeSafeMatcher<Request>() {

            @Override
            public void describeTo(final Description description) {
                description.appendText("request without").appendValueList(" headers named {", ",", "}", headerNames);
            }

            @Override
            protected boolean matchesSafely(final Request item) {
                for (final String headerName : headerNames) {
                    if (item.header(headerName).isPresent()) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    public static Matcher<Request> withHeader(final String name, final String value) {
        return new TypeSafeMatcher<Request>() {

            @Override
            public void describeTo(final Description description) {
                description.appendText("Request with header {").appendValue(name).appendText("=")
                    .appendValue(value).appendText("}");
            }

            @Override
            protected boolean matchesSafely(final Request item) {
                return item.header(name).equals(Optional.ofNullable(value));
            }
        };
    }

    public static Matcher<Request> with(final String method, final String url, final String payload) {
        return new BaseMatcher<Request>() {

            @Override
            public boolean matches(final Object item) {
                if (!(item instanceof Request)) {
                    return false;
                }

                final Request request = (Request) item;
                return url.equals(request.uri()) && method.equals(request.method())
                    && request.payload().isPresent() && payload.equals(request.payload().get().utf8());
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("request").appendText(" method=").appendValue(method).appendText(" url=")
                    .appendValue(url).appendText(" payload=").appendValue(payload);
            }
        };
    }

    public static Matcher<Request> with(final String path, final long ttl) {
        return new TypeSafeMatcher<Request>() {

            @Override
            public void describeTo(final Description description) {
                description.appendText("a request for path=").appendValue(path).appendText(" with ttl=")
                    .appendValue(ttl);
            }

            @Override
            protected boolean matchesSafely(final Request item) {
                return path.equals(item.uri()) && Optional.of(ttl).equals(item.ttl().map(d -> d.toMillis()));
            }
        };
    }
}
