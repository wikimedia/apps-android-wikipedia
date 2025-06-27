package org.wikimedia.metrics_platform.utils;

import java.util.function.Supplier;

public final class Objects {

    private Objects() {
        // Utility class, should not be instantiated
    }

    @Nonnull
    public static <T> T firstNonNull(@Nullable T first, @Nonnull T second) {
        if (first != null) return first;
        return second;
    }

    @Nonnull
    public static <T> T firstNonNull(@Nullable T first, @Nonnull Supplier<T> second) {
        if (first != null) return first;
        T result = second.get();
        if (result == null) throw new NullPointerException("'second' parameter should always create a non null object.");
        return result;
    }
}
