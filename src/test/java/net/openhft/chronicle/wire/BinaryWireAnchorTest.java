//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
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
