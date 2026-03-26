/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.util.Mocker;
import net.openhft.chronicle.core.util.ObjectUtils;
import net.openhft.chronicle.wire.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

// Test class extending WireTestCommon to test method writing and reading via interface implementations
public class MethodWriterByInterfaceTest extends WireTestCommon {

    // Setup method to configure default object creation for interfaces before each test
    @BeforeEach
    void setup() {
        ObjectUtils.defaultObjectForInterface(c -> Class.forName(c.getName() + "mpl"));
    }

    // Teardown method to reset default object creation for interfaces after each test
    @AfterEach
    void teardown() {
        ObjectUtils.defaultObjectForInterface(c -> c);
    }

    // Test method writing and reading via implementation with text wire type
    @Test
    void writeReadViaImplementation() {
        checkWriteReadViaImplementation(WireType.TEXT, false);
    }

    // Test method writing and reading with text wire type and tuple generation enabled
    @Test
    void writeReadViaImplementationGenerateTuples() {
        checkWriteReadViaImplementation(WireType.TEXT, true);
    }

    // Test method writing and reading via implementation with YAML wire type
    @Test
    void writeReadViaImplementationYaml() {
        checkWriteReadViaImplementation(WireType.YAML_ONLY, false);
    }

    // Helper method to perform the core test logic for different wire types
    private void checkWriteReadViaImplementation(WireType wireType, boolean generateTuples) {
        // Create a new wire instance of the specified wire type
        Wire tw = wireType.apply(Bytes.allocateElasticOnHeap()).generateTuples(generateTuples);

        // Create a method writer for the MWBI0 interface
        MWBI0 mwbi0 = tw.methodWriter(MWBI0.class);

        // Write data using the method writer
        mwbi0.method(new MWBImpl("name", 1234567890123456L));

        // Verify that the writer is not a proxy class
        assertFalse(Proxy.isProxyClass(mwbi0.getClass()));

        // Assert the string representation of the wire
        assertEquals("method: {\n" +
                "  name: name,\n" +
                "  time: 2009-02-13T23:31:30.123456\n" +
                "}\n" +
                "...\n", tw.toString());

        // Setup a StringWriter to capture the method reader's output
        StringWriter sw = new StringWriter();

        // Create a method reader and attach a logger to capture output
        MethodReader reader = tw.methodReader(Mocker.logging(MWBI0.class, "", sw));

        // Verify that the reader is not a proxy class
        assertFalse(Proxy.isProxyClass(reader.getClass()));

        // Read data and assert that the reader successfully reads an entry
        assertTrue(reader.readOne());

        // Assert the output captured by the StringWriter
        assertEquals("method[!net.openhft.chronicle.wire.method.MethodWriterByInterfaceTest$MWBImpl {\n" +
                "  name: name,\n" +
                "  time: 2009-02-13T23:31:30.123456\n" +
                "}\n" +
                "]\n", sw.toString().replace("\r", ""));
    }

    // Interface representing a data structure with name and time
    interface MWBI {
        String name();

        long time();
    }

    // Interface representing a method that accepts MWBI type
    interface MWBI0 {
        void method(MWBI mwbi);
    }

    // Implementation of MWBI with name and time fields
    static class MWBImpl extends SelfDescribingMarshallable implements MWBI {
        String name;
        @LongConversion(MicroTimestampLongConverter.class)
        long time;

        // Constructor to initialize name and time
        MWBImpl(String name, long time) {
            this.name = name;
            this.time = time;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public long time() {
            return time;
        }
    }
}
