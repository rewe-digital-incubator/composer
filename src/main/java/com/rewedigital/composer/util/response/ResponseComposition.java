package com.rewedigital.composer.util.response;

import static com.rewedigital.composer.util.Combiners.throwingCombiner;
import static java.util.stream.Collectors.toList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.rewedigital.composer.util.composable.ComposableRoot;
import com.rewedigital.composer.util.request.RequestEnricher;
import com.spotify.apollo.Request;
import com.spotify.apollo.Response;

/**
 * Extension of a {@link ComposedResponse}. Holds multiple
 * {@link ComposableRoot}s that form the base of the extensions. Creates
 * {@link ResponseCompositionFragment}s based on the contained roots.
 *
 */
public class ResponseComposition implements RequestEnricher {

    private final Map<Class<ComposableRoot<?>>, ComposableRoot<?>> roots;

    private ResponseComposition(final Map<Class<ComposableRoot<?>>, ComposableRoot<?>> roots) {
        this.roots = roots;
    }

    public static ResponseComposition of(final List<ComposableRoot<?>> roots) {
        Map<Class<ComposableRoot<?>>, ComposableRoot<?>> mappedRoots = new HashMap<>();
        for (ComposableRoot<?> root : roots) {
            @SuppressWarnings("unchecked")
            Class<ComposableRoot<?>> type = (Class<ComposableRoot<?>>) root.getClass();
            if (mappedRoots.containsKey(type)) {
                throw new IllegalArgumentException(
                        "each type of MergableRoot<> must only appear at most once in parameter roots!");
            }
            mappedRoots.put(type, root);
        }
        return new ResponseComposition(mappedRoots);
    }

    public ResponseComposition composedWithFragmentFor(final Response<?> response) {
        return this.composedWith(fragmentFor(response));
    }

    public ResponseCompositionFragment fragmentFor(final Response<?> response) {
        return new ResponseCompositionFragment(
                roots.values().stream().map(r -> r.composeableFor(response)).collect(toList()));
    }

    public ResponseComposition composedWith(final ResponseCompositionFragment fragment) {
        return ResponseComposition.of(roots.values().stream().map(r -> r.composedFrom(fragment)).collect(toList()));
    }

    @SuppressWarnings("unchecked")
    public <Y extends ComposableRoot<?>> Optional<Y> get(final Class<Y> type) {
        return Optional.ofNullable((Y) roots.get(type));
    }

    @Override
    public Request enrich(Request request) {
        return roots.values().stream().filter(RequestEnricher.class::isInstance).reduce(request,
                (req, root) -> ((RequestEnricher) root).enrich(req), throwingCombiner());
    }

    public <P> Response<P> writeTo(final Response<P> response) {
        return roots.values().stream().reduce(response, (resp, root) -> root.writtenTo(resp), throwingCombiner());

    }
}
