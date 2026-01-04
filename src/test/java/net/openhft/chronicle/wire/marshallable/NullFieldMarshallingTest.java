/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.onoes.ExceptionKey;
import net.openhft.chronicle.wire.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class NullFieldMarshallingTest extends WireTestCommon {
    private Map<ExceptionKey, Integer> exceptions;

    @BeforeEach
    public void setup() {
        exceptions = Jvm.recordExceptions();
    }

    @AfterEach
    @Override
    public void checkExceptions() {
        // find any discarded resources.
        System.gc();
        Jvm.pause(Jvm.isAzulZing() ? 100 : 10);

        if (Jvm.hasException(exceptions)) {
            Jvm.dumpException(exceptions);
            Jvm.resetExceptionHandlers();
            Assertions.fail("Unexpected exceptions recorded during test");
        }
    }

    @Test
    @DisplayName("Null abstract field remains null for text marshalling")
    public void testAbstractNullFieldUnmarshalledCorrectlyText() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for text marshalling test");

        VO object = new VO();

        String val = Marshallable.$toString(object);

        VO object2 = Marshallable.fromString(val);
        assertNotNull(object2, "Text round-trip should return a non-null VO instance");
        assertNull(object2.zoneId, "Text round-trip should keep ZoneId field null");
    }

    @Test
    @DisplayName("Null abstract field remains null for binary marshalling")
    public void testAbstractNullFieldUnmarshalledCorrectlyBinary() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for binary marshalling test");

        VO object = new VO();

        final Wire wire = WireType.BINARY.apply(Bytes.allocateElasticOnHeap());
        wire.write().typedMarshallable(object);

        VO object2 = wire.read().typedMarshallable();
        assertNotNull(object2, "Binary round-trip should return a non-null VO instance");
        assertNull(object2.zoneId, "Binary round-trip should keep ZoneId field null");
        wire.bytes().releaseLast();
    }

    static class VO extends SelfDescribingMarshallable {
        ZoneId zoneId;

        VO() {
            zoneId = null;
        }
    }
}
