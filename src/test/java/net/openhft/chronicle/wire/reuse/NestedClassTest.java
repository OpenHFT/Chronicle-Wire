/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.reuse;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.BinaryWire;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * NestedClassTest is a parameterized test class extending WireTestCommon.
 * It tests the serialization and deserialization of OuterClass instances
 * with different Wire formats.
 */
class NestedClassTest extends WireTestCommon {
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

    // Method to provide different combinations of Wire instances for testing.
    @SuppressWarnings("rawtypes")
    static Collection<Object[]> combinations() {
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
    @ParameterizedTest
    @MethodSource("combinations")
    @DisplayName("Nested OuterClass round-trips across wire types")
    @SuppressWarnings("rawtypes")
    void testMultipleReads(Function<Bytes<?>, Wire> wireType) {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for nested class tests");

        OuterClassWireTestSupport.assertTwoOuterClasses(wireType, OuterClass::new, outerClass1, outerClass2, false);
    }
}
