package org.wikipedia.testutils;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class JunitJupiterCompatUtil {

    private JunitJupiterCompatUtil() {
        // Private constructor to avoid instantiation
    }

    @Deprecated
    public static <T extends Throwable> void assertThrows(Class<T> expectedType, Runnable runnable) {
        boolean success = false;
        Throwable throwable = null;
        try {
            runnable.run();
        } catch (Throwable t) {
            success = expectedType.isInstance(t);
            throwable = t;
        }
        assertTrue(success, "Expected " + expectedType.getName() + " but was " + (throwable == null ? "null" : throwable.getClass().getName()));
    }
}
