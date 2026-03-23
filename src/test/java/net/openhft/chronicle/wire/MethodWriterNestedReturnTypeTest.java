/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This class tests the capability of MethodWriter to handle methods with nested return types.
 * It extends the WireTestCommon for common test setup and utilities.
 */
public class MethodWriterNestedReturnTypeTest extends WireTestCommon {

    /**
     * This test ensures that MethodWriter can handle methods with nested return types without resorting to proxy classes.
     */
    @Test
    public void testNestedReturnTypeIsSupportedInGeneratedWriter() {
        // Initialization of the wire
        BinaryWire binaryWire = new BinaryWire(Bytes.allocateElasticOnHeap(128));

        final Start writer = binaryWire.methodWriter(Start.class);

        // Check if the generated writer is not a proxy class
        // A proxy would indicate a fallback mechanism, suggesting the compilation of the generated writer failed.
        assertFalse(Proxy.isProxyClass(writer.getClass()));
    }

    /**
     * An interface with a method that returns another interface (nested return type).
     */
    interface Start {
        End start();
    }

    /**
     * A simple interface defining an end method.
     */
    interface End {
        void end();
    }
}
