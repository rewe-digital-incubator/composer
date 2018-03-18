package com.rewedigital.composer.util;

import java.util.function.BinaryOperator;

public class Combiners {

    public static <T> BinaryOperator<T> throwingCombiner() {
        return (a, b) -> {
            throw new UnsupportedOperationException("Must not use parallel stream.");
        };
    }

}
