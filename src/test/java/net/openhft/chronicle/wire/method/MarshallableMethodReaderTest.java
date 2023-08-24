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

package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.TextWire;
import net.openhft.chronicle.wire.Wire;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class MarshallableMethodReaderTest extends net.openhft.chronicle.wire.WireTestCommon {
    @Test
    public void test() {
        Wire wire = new TextWire(Bytes.from("say: hi")).useTextDocuments();
        final SayingMicroservice sm = new SayingMicroservice();
        final MethodReader reader = wire.methodReader(sm);
        assertTrue(reader.readOne());
    }

    @Test
    public void ignoredMethods() {
        doIgnoredMethods(false);
    }
    @Test
    public void ignoredMethodsScanning() {
        doIgnoredMethods(true);
    }

    public void doIgnoredMethods(boolean scanning) {
        Wire wire = Wire.newYamlWireOnHeap();
        final SayingMicroservice sm = new SayingMicroservice();
        final MethodReader reader = wire.methodReaderBuilder().scanning(scanning).build(sm);

        writeDoc(wire, "say");
        assertTrue(reader.readOne());

        writeDoc(wire, "bye");

        if (!scanning)
            assertTrue(reader.readOne());

        assertFalse(reader.readOne());

        writeDoc(wire, "bye");
        writeDoc(wire, "say");
        assertTrue(reader.readOne());

        if (!scanning)
            assertTrue(reader.readOne());
        assertFalse(reader.readOne());
    }

    private static void writeDoc(Wire wire, String say) {
        try (DocumentContext dc = wire.writingDocument()) {
            wire.write(say).text("");
        }
    }

    interface Saying {
        void say(String hi);
    }

    static class SayingMicroservice extends SelfDescribingMarshallable implements Saying {
        transient List<String> said = new ArrayList<>();

        @Override
        public void say(String hi) {
            said.add(hi);
        }

        // not called as not on an interface
        public void bye(String reason) {
        }
    }
}
