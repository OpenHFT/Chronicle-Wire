//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.junit.Assert;
import org.junit.Test;

/**
 * Checks that exception raised by {@link ReadMarshallable#unexpectedField(Object, ValueIn)}
 * is thrown back to the user call.
 */
public class UnknownFieldsTest extends WireTestCommon {

    // Static initialization block to add class aliases to ClassAliasPool for the test variations
    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(Variation1.class);
        ClassAliasPool.CLASS_ALIASES.addAlias(Variation2.class);
        ClassAliasPool.CLASS_ALIASES.addAlias(Inner.class);
    }

    // Test to verify if the expected exception is thrown and not suppressed
    @Test
    public void testExceptionIsNotSwallowed() {
        try {
            WireType.TEXT.fromString
                    ("!Variation1 {\n" +
                            "    object: !Inner {\n" +
                            "        unknown: true" +
                            "    }\n" +
                            "}\n");
            Assert.fail();  // If no exception is thrown, the test should fail
        } catch (UnexpectedFieldHandlingException e) {
            // Verify that the cause of the exception is as expected
            Assert.assertEquals(NumberFormatException.class, e.getCause().getClass());
        }
    }

    // Test to verify if the expected exception's transformation is correctly handled
    @Test
    public void testExceptionIsNotTransformed() {
        try {
            WireType.TEXT.fromString
                    ("!Variation2 {\n" +
                            "    object: !Inner {\n" +
                            "        unknown: true\n" +
                            "    }\n" +
                            "}\n");
            Assert.fail();  // If no exception is thrown, the test should fail
        } catch (UnexpectedFieldHandlingException e) {
            // Verify that the cause of the exception is as expected
            Assert.assertEquals(NumberFormatException.class, e.getCause().getClass());
        }
    }

    // Inner class that defines behavior when an unexpected field is encountered
    private static class Inner implements Marshallable {
        @Override
        public void unexpectedField(Object event, ValueIn valueIn) {
            // Throw a NumberFormatException when an unexpected field is encountered
            throw new NumberFormatException();
        }
    }

    // Test variation class with a generic Object field
    private static class Variation1 implements Marshallable {
        Object object;
    }

    // Test variation class with a custom behavior for unexpected fields
    private static class Variation2 implements Marshallable {
        Object object;

        @Override
        public void unexpectedField(Object event, ValueIn valueIn) {
            // Throw an AssertionError with a descriptive message
            throw new AssertionError
                    ("This should not be called with the field name '" + event +
                            "' and value '" + valueIn + "'");
        }
    }
}
