/*
 * Copyright 2016-2025 chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.junit.Test;

import static org.junit.Assert.assertThrows;

public class BinaryWireAnchorTest extends WireTestCommon {

    static class ExposedBinaryWire extends BinaryWire {
        public ExposedBinaryWire(Bytes<?> bytes) {
            super(bytes);
        }

        public void callAnchor() {
            anchor(this);
        }

        public void callFieldAnchor() {
            fieldAnchor(this);
        }
    }

    @Test
    public void anchorMethodsThrowUnexpectedCode() {
        ExposedBinaryWire wire = new ExposedBinaryWire(Bytes.allocateElasticOnHeap());
        assertThrows(IORuntimeException.class, wire::callAnchor);
        assertThrows(IORuntimeException.class, wire::callFieldAnchor);
    }
}
