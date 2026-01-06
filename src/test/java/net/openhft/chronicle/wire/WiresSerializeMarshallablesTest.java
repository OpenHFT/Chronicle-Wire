/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WiresSerializeMarshallablesTest extends WireTestCommon {

    @Test
    @DisplayName("Interfaces do not select a serialisation strategy")
    void interfaceDoesNotSelectStrategy() {
        assertNull(Wires.SerializeMarshallables.INSTANCE.apply(PlainInterface.class),
                "Interfaces without a direct strategy should return null");
    }

    @Test
    @DisplayName("Externalizable types select the externalisable strategy")
    void externalizableSelectsStrategy() {
        assertEquals(SerializationStrategies.EXTERNALIZABLE,
                Wires.SerializeMarshallables.INSTANCE.apply(PlainExternalizable.class),
                "Externalizable types should use the externalisable strategy");
    }

    @Test
    @DisplayName("Comparable types select the scalar strategy")
    void comparableSelectsScalarStrategy() {
        assertEquals(SerializationStrategies.ANY_SCALAR,
                Wires.SerializeMarshallables.INSTANCE.apply(PlainComparable.class),
                "Comparable types should use the scalar strategy");
    }

    private interface PlainInterface {
    }

    private static final class PlainExternalizable implements Externalizable {
        private static final long serialVersionUID = 0L;

        public PlainExternalizable() {
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeInt(0);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException {
            in.readInt();
        }
    }

    private static final class PlainComparable implements Comparable<PlainComparable> {
        @Override
        public int compareTo(PlainComparable other) {
            if (this == other)
                return 0;
            return Integer.compare(System.identityHashCode(this), System.identityHashCode(other));
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }
    }
}
