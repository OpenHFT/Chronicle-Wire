/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class BinaryWireAnchorTest extends WireTestCommon {

    static class ExposedBinaryWire extends BinaryWire {
        public ExposedBinaryWire(Bytes<?> bytes) {
            super(bytes);
        }

        void callAnchor() {
            anchor(this);
        }

        void callFieldAnchor() {
            fieldAnchor(this);
        }
    }

    @Test
    @DisplayName("Anchor helpers throw for unexpected code paths")
    void anchorMethodsThrowUnexpectedCode() {
        ExposedBinaryWire wire = new ExposedBinaryWire(Bytes.allocateElasticOnHeap());
        assertThrows(IORuntimeException.class, wire::callAnchor,
                "anchor() should reject unexpected call");
        assertThrows(IORuntimeException.class, wire::callFieldAnchor,
                "fieldAnchor() should reject unexpected call");
    }
}
