package com.rewedigital.composer.composing;

import java.io.StringWriter;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import com.rewedigital.composer.session.SessionFragment;

/**
 * Contains the data of a (partial) composition.
 *
 * As the composition of content fragment into a web page is done recursively, this class describes one <em>sub
 * tree</em> of the final composition. Compositions are created leaf to root meaning, that first Composition objects for
 * the leafs are created, than the Composition for the parents and so on until the root Composition representing the
 * whole page is created.
 *
 * The composition can be mapped to some result using {@link #map(BiFunction)}. The argument function takes two
 * arguments: the composed html body and the composed session. These values are calculated lazily.
 *
 * Regarding the html content, a Composition consists of the following:
 * <ul>
 * <li>The {@link #template} - this is the html template this Composition represents. It may contain one or more include
 * tags, for each of them a child Composition is available in {@link #children}</li>
 * <li>The {@link #contentRange} - this is the range of the template that should be included into the
 * <em>parent</em></li>
 * <li>The interval {@link #startOffset}, {@link #endOffset} - this describes the part of the <em>parent</em> that
 * should be replaced with <em>this</em> Composition</li>
 * </ul>
 */
class Composition {

    @FunctionalInterface
    public interface Extractor<T> {
        public T extract(String body, SessionFragment session);
    }

    private final List<Composition> children;
    private final List<String> assetLinks;
    private final int startOffset;
    private final int endOffset;
    private final String template;
    private final ContentRange contentRange;
    private final SessionFragment session;

    private Composition(final String template, final ContentRange contentRange, final List<String> assetLinks,
        final List<Composition> children) {
        this(0, template.length(), template, contentRange, assetLinks, SessionFragment.empty(), children);
    }

    private Composition(final int startOffset, final int endOffset, final String template,
        final ContentRange contentRange, final List<String> assetLinks, final SessionFragment session,
        final List<Composition> children) {
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.template = template;
        this.contentRange = contentRange;
        this.assetLinks = assetLinks;
        this.children = children;
        this.session = session;
    }

    public Composition forRange(final int startOffset, final int endOffset) {
        return new Composition(startOffset, endOffset, template, contentRange, assetLinks, session,
            children);
    }

    public Composition withSession(final SessionFragment session) {
        return new Composition(startOffset, endOffset, template, contentRange, assetLinks,
            this.session.mergedWith(session), children);
    }

    public static Composition of(final String template, final ContentRange contentRange,
        final List<String> assetLinks, final List<Composition> children) {
        return new Composition(template, contentRange, assetLinks, children);
    }

    public <R> R map(final BiFunction<String, SessionFragment, R> mapping) {
        return mapping.apply(withAssetLinks(body()), mergedSession());
    }

    public <R> R extract(Extractor<R> extractor) {
        return extractor.extract(withAssetLinks(body()), mergedSession());
    }

    private String body() {
        final StringWriter writer = new StringWriter(template.length());
        int currentIndex = contentRange.start();
        for (final Composition c : children) {
            writer.write(template, currentIndex, c.startOffset - currentIndex);
            writer.write(c.body());
            currentIndex = c.endOffset;
            assetLinks.addAll(c.assetLinks);
        }
        writer.write(template, currentIndex, contentRange.end() - currentIndex);
        return writer.toString();
    }

    private SessionFragment mergedSession() {
        return session.mergedWith(children.stream()
            .reduce(SessionFragment.empty(),
                (s, c) -> s.mergedWith(c.mergedSession()),
                (a, b) -> a.mergedWith(b)));
    }

    private String withAssetLinks(final String body) {
        final String assets = assetLinks.stream()
            .distinct()
            .collect(Collectors.joining("\n"));
        return body.replaceFirst("</head>", assets + "\n</head>");
    }
}
