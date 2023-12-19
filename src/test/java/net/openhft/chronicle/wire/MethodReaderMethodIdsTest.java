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

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodId;
import net.openhft.chronicle.bytes.MethodReader;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class MethodReaderMethodIdsTest extends WireTestCommon {

    @Test
    public void shouldDetermineMethodNamesFromMethodIds() {
        final BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap());
        wire.usePadding(true);

        final Speaker speaker = wire.methodWriterBuilder(Speaker.class).get();
        assertFalse("check we are using generated code", Proxy.isProxyClass(speaker.getClass()));
        speaker.say("hello");

        final AtomicInteger heard = new AtomicInteger();
        final MethodReader reader = new VanillaMethodReaderBuilder(wire).build((Speaker) message -> heard.incrementAndGet());
        assertFalse("check we are using generated code", reader instanceof VanillaMethodReader);
        assertTrue(reader.readOne());
        assertEquals(1, heard.get());
    }

    interface Speaker {
        @MethodId(7)
        void say(final String message);
    }
}
