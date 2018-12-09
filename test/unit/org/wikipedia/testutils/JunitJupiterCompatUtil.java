package org.wikipedia.testutils;

import static org.junit.Assert.assertTrue;

public class JunitJupiterCompatUtil {
    public static <T extends Throwable> void assertThrows(Class<T> expectedType, Runnable runnable) {
        boolean success = false;
        Throwable throwable = null;
        try {
            runnable.run();
        } catch (Throwable t) {
            success = expectedType.isInstance(t);
            throwable = t;
        }
        assertTrue("Expected " + expectedType.getName() + " but was " + (throwable == null ? "null" : throwable.getClass().getName()), success);
    }
}
