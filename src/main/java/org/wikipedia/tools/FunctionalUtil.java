package org.wikipedia.tools;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class FunctionalUtil {
    private FunctionalUtil() {
        // Private constructor to avoid instantiation
    }

    /**
     * As soon as java 8 is no longer supported, this can be replaced by {@link Optional#ifPresentOrElse(Consumer, Runnable)}
     */
    public static <T> void ifPresentOrElse(final Optional<T> optional, final Consumer<T> ifPresent, final Runnable emptyAction) {
        if (optional.isPresent()) {
            ifPresent.accept(optional.get());
        } else {
            emptyAction.run();
        }
    }

    /**
     * As soon as Java 8 is no longer supported, this can be replaced by {@link Optional#or(Supplier)}
     * @return the first optional if it is present, otherwise the result of the given supplier.
     */
    public static <T> Optional<T> or(final Optional<T> optional, final Supplier<Optional<T>> supplier) {
        Objects.requireNonNull(supplier);
        if (optional.isPresent()) {
            return optional;
        }
        return Objects.requireNonNull(supplier.get());
    }
}
