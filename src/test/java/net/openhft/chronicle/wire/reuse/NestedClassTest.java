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
package net.openhft.chronicle.wire.reuse;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.BinaryWire;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;

/**
 * NestedClassTest is a parameterized test class extending WireTestCommon.
 * It tests the serialization and deserialization of OuterClass instances
 * with different Wire formats.
 */
@RunWith(value = Parameterized.class)
public class NestedClassTest extends WireTestCommon {
    // Static instances of OuterClass for testing.
    private static final OuterClass outerClass1 = new OuterClass();
    private static final OuterClass outerClass2 = new OuterClass();

    // Static initializer to configure the test instances of OuterClass.
    static {
        // Setting up outerClass1 and outerClass2 with different values and WireTypes.
        outerClass1.setText("text1");
        outerClass2.setText("text2");
        outerClass1.setWireType(WireType.BINARY);
        outerClass2.setWireType(WireType.TEXT);

        // Initializing the lists within each OuterClass instance.
        outerClass1.clearListA();
        outerClass2.clearListA();
        outerClass1.clearListB();
        outerClass2.clearListB();

        // Adding elements to the lists in each OuterClass instance.
        outerClass1.addListA().setTextNumber("num1A", 11);
        outerClass1.addListB().setTextNumber("num1B", 12);
        outerClass1.addListA().setTextNumber("num1AA", 111);
        outerClass1.addListB().setTextNumber("num1BB", 122);
        outerClass2.addListA().setTextNumber("num2A", 21);
        outerClass2.addListB().setTextNumber("num2B", 22);
    }

    // Function to create Wire instances for each test run.
    @SuppressWarnings("rawtypes")
    private final Function<Bytes<?>, Wire> wireType;

    // Constructor to inject the Wire creation function.
    @SuppressWarnings("rawtypes")
    public NestedClassTest(Function<Bytes<?>, Wire> wireType) {
        this.wireType = wireType;
    }

    // Method to provide different combinations of Wire instances for testing.
    @SuppressWarnings("rawtypes")
    @Parameterized.Parameters
    public static Collection<Object[]> combinations() {
        return Arrays.asList(
                new Object[]{(Function<Bytes<?>, Wire>) bytes -> new BinaryWire(bytes, false, true, false, 128, "binary")},
                new Object[]{WireType.TEXT},
                new Object[]{WireType.YAML_ONLY},
                new Object[]{WireType.BINARY},
                new Object[]{WireType.BINARY_LIGHT},
                new Object[]{WireType.FIELDLESS_BINARY},
                new Object[]{WireType.JSON}
        );
    }

    // Test method to verify multiple reads of OuterClass instances.
    @SuppressWarnings("rawtypes")
    @Test
    public void testMultipleReads() {
        Bytes<?> bytes = Bytes.elasticByteBuffer();
        Wire wire = wireType.apply(bytes);

        // Writing two OuterClass instances to the wire.
        wire.writeEventName(() -> "test1").marshallable(outerClass1);
        if (wireType == WireType.JSON)
            wire.bytes().writeUnsignedByte('\n');
        wire.writeEventName(() -> "test2").marshallable(outerClass2);

       // System.out.println(bytes.readByte(0) < 0 ? bytes.toHexString() : bytes.toString());
       // StringBuilder to capture event names during reading.
        @NotNull StringBuilder sb = new StringBuilder();
        @NotNull OuterClass outerClass0 = new OuterClass();

        // Reading and verifying the first OuterClass instance.
        wire.readEventName(sb).marshallable(outerClass0);
        assertEquals("test1", sb.toString());
        assertEquals(outerClass1.toString(), outerClass0.toString());

        // Reading and verifying the second OuterClass instance.
        wire.readEventName(sb).marshallable(outerClass0);
        assertEquals("test2", sb.toString());
        assertEquals(outerClass2.toString(), outerClass0.toString());

        // Releasing allocated bytes.
        bytes.releaseLast();
    }
}
