package com.rewedigital.composer.composing;

import static com.rewedigital.composer.parser.Parser.PARSER;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.Test;

public class IncludeMarkupHandlerTest {

    @Test
    public void extractsTtlAndPathAttributes() {
        final IncludeMarkupHandler handler = parse("<include path=\"value\" ttl=123>Fallback</include>");

        assertThat(handler.includedServices()).isNotEmpty();
        assertThat(handler.includedServices()).allSatisfy(included -> {
            assertThat(included.ttl()).isPresent().contains(Duration.ofMillis(123));
            assertThat(included.path()).contains("value");
        });

    }

    private IncludeMarkupHandler parse(final String data) {
        final ComposerHtmlConfiguration configuration = new ComposerHtmlConfiguration("include", "content", "asset", 1);
        final IncludeMarkupHandler markupHandler =
            new IncludeMarkupHandler(ContentRange.allUpToo(data.length()), configuration);
        PARSER.parse(data, markupHandler);
        return markupHandler;
    }
}
