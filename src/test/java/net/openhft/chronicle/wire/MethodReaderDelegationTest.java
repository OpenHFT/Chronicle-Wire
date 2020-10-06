/*
 * Copyright 2016-2020 Chronicle Software
 *
 * https://chronicle.software
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
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.Mocker;
import org.junit.Test;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class MethodReaderDelegationTest {
    @Test
    public void testUnsuccessfulCallIsDelegated() {
        final BinaryWire wire = new BinaryWire(Bytes.allocateElasticOnHeap());

        final MyInterface writer = wire.methodWriter(MyInterface.class);
        writer.myCall();

        try (DocumentContext dc = wire.acquireWritingDocument(false)) {
            Objects.requireNonNull(dc.wire()).writeEventName("myFall").text("");
        }

        AtomicReference<String> delegatedMethodCall = new AtomicReference<>();
        StringBuilder sb = new StringBuilder();

        final MethodReader reader = wire.methodReaderBuilder()
                .defaultParselet((s, in) -> { // Default parselet handling is delegated to Vanilla reader.
                    delegatedMethodCall.set(s.toString());
                    in.skipValue();
                })
                .build(Mocker.intercepting(MyInterface.class, "*", sb::append));

        System.out.println(WireDumper.of(wire).asString());

        assertTrue(reader.readOne());
        assertNull(delegatedMethodCall.get());

        assertTrue(reader.readOne());
        assertEquals("myFall", delegatedMethodCall.get());

        assertEquals("*myCall[]", sb.toString());
    }

    interface MyInterface {
        void myCall();
    }
}
