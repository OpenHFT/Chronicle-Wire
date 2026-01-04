/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Checks that exception raised by {@link ReadMarshallable#unexpectedField(Object, ValueIn)}
 * is thrown back to the user call.
 */
@SuppressWarnings({"deprecation", "removal"})
public class UnknownFieldsTest extends WireTestCommon {

    // Static initialization block to add class aliases to ClassAliasPool for the test variations
    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(Variation1.class);
        ClassAliasPool.CLASS_ALIASES.addAlias(Variation2.class);
        ClassAliasPool.CLASS_ALIASES.addAlias(Inner.class);
    }

    // Test to verify if the expected exception is thrown and not suppressed
    @Test
    @DisplayName("Propagates unexpected field exception without swallowing")
    public void testExceptionIsNotSwallowed() {
        try {
            WireType.TEXT.fromString("!Variation1 {\n" +
                    "    object: !Inner {\n" +
                    "        unknown: true" +
                    "    }\n" +
                    "}\n");
            Assertions.fail("UnexpectedFieldHandlingException should be thrown for Variation1");
        } catch (UnexpectedFieldHandlingException e) {
            // Verify that the cause of the exception is as expected
            Assertions.assertEquals(NumberFormatException.class, e.getCause().getClass(),
                    "NumberFormatException cause should be reported for Variation1, but was " + e.getCause());
        }
    }

    // Test to verify if the expected exception's transformation is correctly handled
    @Test
    @DisplayName("Propagates unexpected field exception without transformation")
    public void testExceptionIsNotTransformed() {
        try {
            WireType.TEXT.fromString("!Variation2 {\n" +
                    "    object: !Inner {\n" +
                    "        unknown: true\n" +
                    "    }\n" +
                    "}\n");
            Assertions.fail("UnexpectedFieldHandlingException should be thrown for Variation2");
        } catch (UnexpectedFieldHandlingException e) {
            // Verify that the cause of the exception is as expected
            Assertions.assertEquals(NumberFormatException.class, e.getCause().getClass(),
                    "NumberFormatException cause should be reported for Variation2, but was " + e.getCause());
        }
    }

    // Inner class that defines behaviour when an unexpected field is encountered
    private static class Inner implements Marshallable {
        @Override
        public void unexpectedField(Object event, ValueIn valueIn) {
            // Throw a NumberFormatException when an unexpected field is encountered
            throw new NumberFormatException("Unexpected field in test fixture");
        }
    }

    // Test variation class with a generic Object field
    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    private static class Variation1 implements Marshallable {
        Object object;
    }

    // Test variation class with a custom behavior for unexpected fields
    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    private static class Variation2 implements Marshallable {
        Object object;

        @Override
        public void unexpectedField(Object event, ValueIn valueIn) {
            // Throw an AssertionError with a descriptive message
            throw new AssertionError("This should not be called with the field name '" + event +
                    "' and value '" + valueIn + "'");
        }
    }
}
