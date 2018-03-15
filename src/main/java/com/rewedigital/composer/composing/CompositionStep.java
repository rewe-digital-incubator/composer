package com.rewedigital.composer.composing;

import java.util.Objects;

/**
 * Describes a step in the composition process. The composition is done recursively, and steps are created root to leaf.
 * It is used to trace the progress of the recursive composition. The composition data itself is handled via
 * {@link Composition}.
 */
public class CompositionStep {

    private static final CompositionStep root = new CompositionStep(null, "", 0);

    private final CompositionStep parent;
    private final String path;
    private final int depth;

    public static CompositionStep root(final String path) {
        return new CompositionStep(root, path, 0);
    }

    private CompositionStep(final CompositionStep parent, final String path, final int depth) {
        this.parent = parent;
        this.path = path;
        this.depth = depth;
    }

    public int depth() {
        return depth;
    }

    public CompositionStep childWith(final String path) {
        return new CompositionStep(this, path, depth + 1);
    }

    public String callStack() {
        if (this == root) {
            return "[root]";
        }

        return "[" + path + "] included via " + parent.callStack();
    }

    @Override
    public String toString() {
        return callStack();
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent, path, depth);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CompositionStep other = (CompositionStep) obj;
        return depth == other.depth &&
            Objects.equals(path, other.path) &&
            Objects.equals(parent, other.parent);
    }


}
