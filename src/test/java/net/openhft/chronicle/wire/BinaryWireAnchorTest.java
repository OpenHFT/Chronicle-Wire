/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

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
    @DisplayName("Anchor helpers throw for unexpected code paths")
    public void anchorMethodsThrowUnexpectedCode() {
        ExposedBinaryWire wire = new ExposedBinaryWire(Bytes.allocateElasticOnHeap());
        assertThrows(IORuntimeException.class, wire::callAnchor,
                "Expected anchor() to reject unexpected call");
        assertThrows(IORuntimeException.class, wire::callFieldAnchor,
                "Expected fieldAnchor() to reject unexpected call");
    }
}
