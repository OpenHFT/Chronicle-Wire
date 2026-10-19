/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.openhft.chronicle.wire.VanillaMethodWriterBuilder.DISABLE_WRITER_PROXY_CODEGEN;
import static org.junit.Assert.*;

public class MethodWriterClosedOutputTest extends WireTestCommon {
    public interface ColdEvents { void event(String text); }
    public interface WarmEvents { void event(String text); }
    public interface ProxyEvents { void event(String text); }

    private String previousCodegenProperty;

    @Before
    public void enableCodegen() {
        previousCodegenProperty = System.getProperty(DISABLE_WRITER_PROXY_CODEGEN);
        System.setProperty(DISABLE_WRITER_PROXY_CODEGEN, "false");
    }

    @After
    public void restoreCodegenProperty() {
        if (previousCodegenProperty == null)
            System.clearProperty(DISABLE_WRITER_PROXY_CODEGEN);
        else
            System.setProperty(DISABLE_WRITER_PROXY_CODEGEN, previousCodegenProperty);
    }

    @Test
    public void closedOutputDuringFirstConstructionMustNotPublishAProxy() {
        assertClosedOutputFails(ColdEvents.class);
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            Wire wire = new BinaryWire(bytes);
            ColdEvents writer = wire.methodWriter(ColdEvents.class);
            assertFalse("an output failure must not poison the generated class cache",
                    Proxy.isProxyClass(writer.getClass()));
            writer.event("after failure");
            List<String> seen = new ArrayList<>();
            try (MethodReader reader = wire.methodReader((ColdEvents) seen::add)) {
                assertTrue(reader.readOne());
                assertFalse(reader.readOne());
            }
            assertEquals(Collections.singletonList("after failure"), seen);
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void closedOutputMustAlsoFailWhenTheWriterClassIsAlreadyGenerated() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            WarmEvents writer = new BinaryWire(bytes).methodWriter(WarmEvents.class);
            assertFalse(Proxy.isProxyClass(writer.getClass()));
            assertClosedOutputFails(WarmEvents.class);
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void explicitlyDisabledCodegenStillUsesAWorkingProxy() {
        System.setProperty(DISABLE_WRITER_PROXY_CODEGEN, "true");
        ignoreException("Falling back to proxy method writer");
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            Wire wire = new BinaryWire(bytes);
            ProxyEvents writer = wire.methodWriter(ProxyEvents.class);
            assertTrue(Proxy.isProxyClass(writer.getClass()));
            writer.event("proxy control");
            List<String> seen = new ArrayList<>();
            try (MethodReader reader = wire.methodReader((ProxyEvents) seen::add)) {
                assertTrue(reader.readOne());
                assertFalse(reader.readOne());
            }
            assertEquals(Collections.singletonList("proxy control"), seen);
        } finally {
            bytes.releaseLast();
        }
    }

    private <T> void assertClosedOutputFails(Class<T> type) {
        ClosedIllegalStateException closed = new ClosedIllegalStateException("output closed during construction");
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            VanillaMethodWriterBuilder<T> builder = (VanillaMethodWriterBuilder<T>)
                    new BinaryWire(bytes).methodWriterBuilder(type);
            // Model an output supplier failing after code generation, without introducing a Queue dependency.
            builder.marshallableOutSupplier(() -> { throw closed; });
            try {
                builder.build();
                fail("closed output must be reported to the caller, not replaced with a proxy");
            } catch (ClosedIllegalStateException actual) {
                assertSame("preserve the original close diagnostic", closed, actual);
            }
        } finally {
            bytes.releaseLast();
        }
    }
}
