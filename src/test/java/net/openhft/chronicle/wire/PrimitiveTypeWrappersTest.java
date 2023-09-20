/*
 * Copyright 2016-2020 chronicle.software
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

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@SuppressWarnings("rawtypes")
@RunWith(value = Parameterized.class)
public class PrimitiveTypeWrappersTest extends WireTestCommon {

    private boolean isTextWire;  // Variable to determine if using text wire format

    // Constructor to initialize the isTextWire flag
    public PrimitiveTypeWrappersTest(Object isTextWire) {
        this.isTextWire = (Boolean) isTextWire;
    }

    // Provide parameters to run the tests with
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[]{Boolean.TRUE},
                new Object[]{Boolean.TRUE}  // This seems redundant; consider having diverse values
        );
    }

    // Test writing numbers of different types and reading them back
    @SuppressWarnings("unchecked")
    @Test
    public void testNumbers() {
        // Define wrapper classes for numbers
        @NotNull final Class[] types = new Class[]{Byte.class,
                Short.class, Float.class,
                Integer.class, Long.class, Double.class};
        // Create an array of number instances
        @NotNull final Number[] nums = new Number[]{(byte) 1, (short) 2, (float) 3, 4, (long) 5, (double) 6};

        for (@NotNull Number num : nums) {
            for (@NotNull Class type : types) {
                @NotNull final Wire wire = wireFactory();  // Get a Wire instance

                wire.write().object(num); // Write the number to the wire
               // System.out.println(wire);
                @Nullable final Object object = wire.read().object(type); // Read the number back as the specified type
                Assert.assertTrue(num.getClass() + " to " + type.getName(), type.isAssignableFrom(object.getClass()));
                Assert.assertEquals(num.getClass() + " to " + type.getName(), num.intValue(), ((Number) object).intValue());
            }
        }
    }

    // Test that writing and reading the number maintains the original type
    @Test
    public void testNumbers2() {
        @NotNull final Number[] nums = new Number[]{(byte) 1, (short) 1, (float) 1, 1, (long) 1, (double) 1};

        for (@NotNull Number num : nums) {
            @NotNull final Wire wire = wireFactory();

            wire.write().object(num);
           // System.out.println(num.getClass() + " of " + num + " is " + (isTextWire ? wire.toString() : wire.bytes().toHexString()));
            @Nullable final Object object = wire.read().object(Object.class);
            Assert.assertSame(num.getClass(), object.getClass());
            Assert.assertEquals(num.getClass().getName(), num, object);
        }
    }

    // Test writing and reading a character
    @Test
    public void testCharacter() {
        @NotNull final Wire wire = wireFactory();
        wire.write().object('1');
        @Nullable final Object object = wire.read().object(Character.class);
        Assert.assertTrue(object instanceof Character);
        Assert.assertEquals('1', object);
    }

    // Test writing a string and reading it back as a character
    @Test
    public void testCharacterWritenAsString() {
        @NotNull final Wire wire = wireFactory();
        wire.write().object("1");
        @Nullable final Object object = wire.read().object(Character.class);
        Assert.assertTrue(object instanceof Character);
        Assert.assertEquals('1', object);
    }

    // Test writing a character and reading it back as a string
    @Test
    public void testCharReadAsString() {
        @NotNull final Wire wire = wireFactory();
        wire.write().object('1');
        @Nullable final Object object = wire.read().object(String.class);
        Assert.assertTrue(object instanceof String);
        Assert.assertEquals("1", object);
    }

    // Test writing a long string and reading just the first character
    @Test
    public void testStoreStringReadAsChar() {
        @NotNull final Wire wire = wireFactory();
        wire.write().object("LONG STRING");
        @Nullable final Object object = wire.read().object(Character.class);
        Assert.assertTrue(object instanceof Character);
        Assert.assertEquals('L', object);
    }

    // Helper method to create and return a Wire instance based on the isTextWire flag
    @NotNull
    private Wire wireFactory() {
        @NotNull final Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        @NotNull Wire wire = (isTextWire) ? WireType.TEXT.apply(bytes) : new BinaryWire(bytes);

        return wire;
    }
}
