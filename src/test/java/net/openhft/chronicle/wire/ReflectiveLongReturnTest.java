/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import static org.junit.Assert.*;

import net.openhft.chronicle.bytes.*;

import org.junit.Test;

import java.lang.annotation.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;

public class ReflectiveLongReturnTest extends WireTestCommon {

    public interface LongCall {
        void number(long value);
    }

    public interface Begin {
        End number(long value);
    }

    public interface End {
        void end(String text);
    }

    public interface Converted {
        void number(@LongConversion(HexadecimalLongConverter.class) long value);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    @LongConversion(HexadecimalLongConverter.class)
    public @interface Hex {}

    public interface MetaConverted {
        void number(@Hex long value);
    }

    public interface DateCall {
        void date(LocalDate date);
    }

    private void restore(String key, String value) {
        if (value == null) System.clearProperty(key);
        else System.setProperty(key, value);
    }

    private void textRead(Object handler, String text, boolean reflective) {
        String key = VanillaMethodReaderBuilder.DISABLE_READER_PROXY_CODEGEN,
                old = System.getProperty(key);
        Bytes<?> bytes = Bytes.from(text);
        try {
            System.setProperty(key, Boolean.toString(reflective));
            TextWire wire = new TextWire(bytes);
            wire.useTextDocuments();
            MethodReader reader =
                    new VanillaMethodReaderBuilder(wire).wireType(WireType.TEXT).build(handler);
            try {
                assertEquals(reflective, reader instanceof VanillaMethodReader);
                assertTrue(reader.readOne());
                assertFalse(reader.readOne());
            } finally {
                reader.close();
            }
        } finally {
            restore(key, old);
            bytes.releaseLast();
        }
    }

    @Test
    public void reflectiveLongDispatches() {
        AtomicLong seen = new AtomicLong();
        textRead((LongCall) seen::set, "number: 42\n", true);
        assertEquals(42, seen.get());
    }

    @Test
    public void reflectiveFluentReturnDispatches() {
        List<String> seen = new ArrayList<>();
        Begin begin = n -> s -> seen.add(n + ":" + s);
        textRead(begin, "number: 42\nend: done\n", true);
        assertEquals(Collections.singletonList("42:done"), seen);
    }

    @Test
    public void reflectiveConvertedLongDispatches() {
        AtomicLong seen = new AtomicLong();
        textRead((Converted) seen::set, "number: ff\n", true);
        assertEquals(255, seen.get());
    }

    @Test
    public void generatedConvertedLongDispatches() {
        AtomicLong seen = new AtomicLong();
        textRead((Converted) seen::set, "number: ff\n", false);
        assertEquals(255, seen.get());
    }

    @Test
    public void generatedMetaConvertedLongDispatches() {
        AtomicLong seen = new AtomicLong();
        textRead((MetaConverted) seen::set, "number: ff\n", false);
        assertEquals(255, seen.get());
    }

    @Test
    public void v2WriterImportsExternalScalar() throws Exception {
        String key = "wire.generator.v2", old = System.getProperty(key);
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            System.setProperty(key, "true");
            Wire wire = new BinaryWire(bytes);
            DateCall writer = wire.methodWriter(DateCall.class);
            AtomicReference<LocalDate> seen = new AtomicReference<>();
            MethodReader reader = wire.methodReader((DateCall) seen::set);
            try {
                LocalDate date = LocalDate.of(2026, 11, 16);
                writer.date(date);
                assertTrue(reader.readOne());
                assertEquals(date, seen.get());
            } finally {
                reader.close();
            }
        } finally {
            restore(key, old);
            bytes.releaseLast();
        }
    }
}
