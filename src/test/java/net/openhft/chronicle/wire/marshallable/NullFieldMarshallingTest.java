/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.onoes.ExceptionKey;
import net.openhft.chronicle.wire.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.ZoneId;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeFalse;

public class NullFieldMarshallingTest extends WireTestCommon {
    protected Map<ExceptionKey, Integer> exceptions;

    @Before
    public void setup() {
        exceptions = Jvm.recordExceptions();
    }

    @After
    public void checkExceptions() {
        // find any discarded resources.
        System.gc();
        Jvm.pause(Jvm.isAzulZing() ? 100 : 10);

        if (Jvm.hasException(exceptions)) {
            Jvm.dumpException(exceptions);
            Jvm.resetExceptionHandlers();
            Assert.fail();
        }
    }

    @Test
    public void testAbstractNullFieldUnmarshalledCorrectlyText() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        VO object = new VO();

        String val = Marshallable.$toString(object);

        VO object2 = Marshallable.fromString(val);
        assertNotNull(object2);
        assertNull(object2.zoneId);
    }

    @Test
    public void testAbstractNullFieldUnmarshalledCorrectlyBinary() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        VO object = new VO();

        final Wire wire = WireType.BINARY.apply(Bytes.allocateElasticOnHeap());
        wire.write().typedMarshallable(object);

        VO object2 = wire.read().typedMarshallable();
        assertNotNull(object2);
        assertNull(object2.zoneId);
        wire.bytes().releaseLast();
    }

    static class VO extends SelfDescribingMarshallable {
        ZoneId zoneId;
    }
}
