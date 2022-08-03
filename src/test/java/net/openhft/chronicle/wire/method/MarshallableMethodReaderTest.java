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
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.TextWire;
import net.openhft.chronicle.wire.Wire;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MarshallableMethodReaderTest {
    @Test
    public void test() {
        Wire wire = new TextWire(Bytes.from("say: hi")).useTextDocuments();
        final SayingMicroservice sm = new SayingMicroservice();
        final MethodReader reader = wire.methodReader(sm);
        assertTrue(reader.readOne());
    }

    @Test
    public void ignoredMethods() {
        Wire wire = Wire.newYamlWireOnHeap();
        final SayingMicroservice sm = new SayingMicroservice();
        final MethodReader reader = wire.methodReader(sm);
        for (Method method : sm.getClass().getMethods()) {
            final String name = method.getName();
            wire.write(name).text("");
            assertTrue(method.toString(), reader.readOne());
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
    }
}
