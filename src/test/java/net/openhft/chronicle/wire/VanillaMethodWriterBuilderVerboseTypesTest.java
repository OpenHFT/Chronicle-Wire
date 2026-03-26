/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

class VanillaMethodWriterBuilderVerboseTypesTest extends net.openhft.chronicle.wire.WireTestCommon {

    // Static initialization block to alias two classes
    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(MyObject.class, MyObject2.class);
    }

    // Flag to determine if verbose types should be used
    private boolean verboseTypes;

    // Expected string representation for the current test run
    private String expects;

    @BeforeEach
    void hasDirect() {
        assumeFalse(Jvm.maxDirectMemory() == 0);
    }

    // Provide different combinations of parameters for the test runs
    @NotNull
    public static Collection<Object[]> combinations() {
        return Arrays.asList(new Object[]{true, "print: !MyObject {\n" +
                "  list: [\n" +
                "    { str: hello world, value: 23 }\n" +
                "  ]\n" +
                "}\n" +
                "...\n"}, new Object[]{false, "print: {\n" +
                "  list: [\n" +
                "    { str: hello world, value: 23 }\n" +
                "  ]\n" +
                "}\n" +
                "...\n"});
    }

    // Nested class representing a specific object with a string and value
    static class MyObject2 extends SelfDescribingMarshallable {

        private final String str;
        private final int value;

        MyObject2(String str, int value) {
            this.str = str;
            this.value = value;
        }
    }

    // Nested class representing an object containing a list of `MyObject2`
    static class MyObject extends SelfDescribingMarshallable {

        private final ArrayList<MyObject2> list = new ArrayList<>();

        MyObject(String str, int value) {
            list.add(new MyObject2(str, value));
        }
    }

    // Interface defining a printing method
    interface Printer {
        void print(MyObject msg);
    }

    // Test case to validate the output of the method writer based on the verbose types setting
    @ParameterizedTest
    @MethodSource("combinations")
    void test(boolean verboseTypes, String expects) {
        this.verboseTypes = verboseTypes;
        this.expects = expects;
        // Allocate elastic bytes on heap and create a TextWire instance
        final Bytes<byte[]> bytes = Bytes.allocateElasticOnHeap();
        TextWire textWire = new TextWire(bytes);

        // Configure method writer builder with verbosity settings
        VanillaMethodWriterBuilder<Printer> methodWriterBuilder = (VanillaMethodWriterBuilder<Printer>) textWire.methodWriterBuilder(false, Printer.class);
        methodWriterBuilder.verboseTypes(verboseTypes);

        // Create a printer instance and print a message
        Printer printer = methodWriterBuilder.build();
        printer.print(new MyObject("hello world", 23));

        // Assert that the output matches the expected representation for the current run
        assertEquals(expects, bytes.toString());
    }
}
