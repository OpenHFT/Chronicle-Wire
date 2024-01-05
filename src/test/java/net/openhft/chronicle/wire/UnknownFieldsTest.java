/*
 * Copyright 2016-2020 Chronicle Software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    public static class Inner implements Marshallable {
        @Override
        public void unexpectedField(Object event, ValueIn valueIn) {
            // Throw a NumberFormatException when an unexpected field is encountered
            throw new NumberFormatException();
        }
    }

    // Test variation class with a generic Object field
    public static class Variation1 implements Marshallable {
        Object object;
    }

    // Test variation class with a custom behavior for unexpected fields
    public static class Variation2 implements Marshallable {
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
