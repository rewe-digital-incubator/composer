package com.rewedigital.composer.composing;

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
}
