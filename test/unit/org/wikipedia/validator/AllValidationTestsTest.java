// License: GPL. For details, see LICENSE file.
package org.wikipedia.validator;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.TestError;

class AllValidationTestsTest {

    @Test
    void testValidationTestFields() throws IllegalAccessException {
        final Field[] fields = AllValidationTests.class.getDeclaredFields();
        for (Field field : fields) {
            if ((field.getModifiers() & Modifier.PRIVATE) == 0) {
                final Object fieldValue = field.get(null);
                if (fieldValue instanceof AllValidationTests.ValidationTest) {
                    System.out.print("Check validation test " + field.getName());
                    testValidationTestField((AllValidationTests.ValidationTest<?>) fieldValue);
                }
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void testValidationTestField(final AllValidationTests.ValidationTest validationTest) {
        final TestError te = validationTest
            .getBuilder(new org.openstreetmap.josm.data.validation.Test("DummyTest"))
            .message("dummy message")
            .primitives(new Way())
            .build();
        assertTrue(te.getCode() >= 30_000, "The code of a validation test must be at least 30,000");
        assertTrue(te.getCode() < 31_000, "The code of a validation test must be lower than 31,000");
        assertNotNull(te.getSeverity(), "The severity of a validation test must be non-null");
        System.out.println(" \uD83D\uDDF8");
    }
}
