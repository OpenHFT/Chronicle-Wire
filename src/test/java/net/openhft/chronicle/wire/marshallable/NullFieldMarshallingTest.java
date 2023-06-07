/*
 * Copyright 2016-2022 chronicle.software
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

package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.onoes.ExceptionKey;
import net.openhft.chronicle.wire.*;
import net.openhft.chronicle.wire.scoped.ScopedResource;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.ZoneId;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
        VO object = new VO();

        String val = Marshallable.$toString(object);

        VO object2 = Marshallable.fromString(val);
        assertNotNull(object2);
        assertNull(object2.zoneId);
    }

    @Test
    public void testAbstractNullFieldUnmarshalledCorrectlyBinary() {
        VO object = new VO();
        try (final ScopedResource<Wire> wireSR = Wires.acquireBinaryWireScoped()) {
            Wire wire = wireSR.get();
            wire.write().typedMarshallable(object);

            VO object2 = wire.read().typedMarshallable();
            assertNotNull(object2);
            assertNull(object2.zoneId);
        }
    }

    static class VO extends SelfDescribingMarshallable {
        ZoneId zoneId;
    }
}
