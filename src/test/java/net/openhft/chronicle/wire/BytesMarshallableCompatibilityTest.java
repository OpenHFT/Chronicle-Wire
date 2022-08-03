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
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;

public final class BytesMarshallableCompatibilityTest extends WireTestCommon {

    @Test
    public void shouldSerialiseToBytes() {
        final Container container = new Container();
        container.number = 17;
        container.label = "non-deterministic";
        container.truth = Boolean.TRUE;

        final Bytes<ByteBuffer> bytes = Bytes.elasticHeapByteBuffer(64);

        container.writeMarshallable(bytes);

        final Container copy = new Container();
        copy.readMarshallable(bytes);

        assertEquals(container.number, copy.number);
        assertEquals(container.label, copy.label);
        assertEquals(container.truth, copy.truth);
    }

    private static final class Container extends BytesInBinaryMarshallable {
        private int number;
        private String label;
        private Boolean truth;
    }
}
