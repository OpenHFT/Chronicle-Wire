/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class BracketsOnJSONWireTest extends net.openhft.chronicle.wire.WireTestCommon {

    // Variable to store the actual message from the wire
    private String actual;
    private String previousDisableProxyCodegen;

    // Interface to define a Printer with a single method 'print'
    public interface Printer {
        void print(String msg);
    }

    @BeforeEach
    void enableWriterProxyCodegen() {
        previousDisableProxyCodegen = System.getProperty(VanillaMethodWriterBuilder.DISABLE_WRITER_PROXY_CODEGEN);
        System.setProperty(VanillaMethodWriterBuilder.DISABLE_WRITER_PROXY_CODEGEN, "false");
    }

    @AfterEach
    void restoreWriterProxyCodegenSetting() {
        if (previousDisableProxyCodegen == null) {
            System.clearProperty(VanillaMethodWriterBuilder.DISABLE_WRITER_PROXY_CODEGEN);
        } else {
            System.setProperty(VanillaMethodWriterBuilder.DISABLE_WRITER_PROXY_CODEGEN, previousDisableProxyCodegen);
        }
    }

    // Test the JSON_ONLY wire type with a method writer and reader using the Printer interface
    @Test
    @DisplayName("JSON_ONLY writes without brackets and reads back")
    void test() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "direct memory required for JSON_ONLY wire test");

        // Create an elastic byte buffer to hold the wire data
        final Bytes<ByteBuffer> t = Bytes.elasticByteBuffer();

        // Initialize the wire with JSON_ONLY type and apply it to the buffer
        Wire wire = WireType.JSON_ONLY.apply(t);

        // Use a method writer to write a print message to the wire
        wire.methodWriter(Printer.class)
                .print("hello");

        // Assert that the wire representation matches the expected JSON format
        assertEquals("{\"print\":\"hello\"}", wire.toString(),
                "JSON_ONLY should write single field without surrounding braces");

        // Use a method reader to read the message from the wire and set the 'actual' variable
        wire.methodReader((Printer) msg -> actual = msg).readOne();

        // Release the buffer to free up resources
        t.releaseLast();

        // Assert that the read message matches the original message written to the wire
        assertEquals("hello", actual, "method reader should receive the printed message");
    }
}
