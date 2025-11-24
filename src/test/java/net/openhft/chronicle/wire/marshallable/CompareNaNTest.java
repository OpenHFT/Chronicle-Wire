//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.WireTestCommon;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

/**
 * This class tests the behavior of NaN values when used in different data types like primitives, wrappers, and objects.
 * The tests emphasize the importance of consistent behavior when comparing such entities containing NaN.
 */
public class CompareNaNTest extends WireTestCommon {

    /**
     * Test the comparison behavior for primitive data types containing NaN values.
     * Ensures that two DTOs with NaN primitive values are considered equal.
     */
    @Test
    public void testPrim() {
        @NotNull PrimDTO a = new PrimDTO(Double.NaN, Float.NaN);
        @NotNull PrimDTO b = new PrimDTO(Double.NaN, Float.NaN);
        assertEquals(a.toString(), b.toString());
        assertEquals(a, b);
    }

    /**
     * Test the comparison behavior for wrapper data types containing NaN values.
     * Ensures that two DTOs with NaN wrapped values are considered equal.
     */
    @Test
    public void testWrapDTO() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        @NotNull WrapDTO a = new WrapDTO(Double.NaN, Float.NaN);
        @NotNull WrapDTO b = new WrapDTO(Double.NaN, Float.NaN);
        assertEquals(a.toString(), b.toString());
        assertEquals(a, b);
    }

    /**
     * Test the comparison behavior for objects containing NaN values.
     * Ensures that two DTOs with NaN object values are considered equal.
     */
    @Test
    public void testObjectWrapDTO() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        @NotNull ObjectWrapDTO a = new ObjectWrapDTO(Double.NaN, Float.NaN);
        @NotNull ObjectWrapDTO b = new ObjectWrapDTO(Double.NaN, Float.NaN);
        assertEquals(a.toString(), b.toString());
        assertEquals(a, b);
    }

    /**
     * A Data Transfer Object (DTO) representing primitive data types.
     */
    static class PrimDTO extends SelfDescribingMarshallable {
        double d;
        float f;

        PrimDTO(double d, float f) {
            this.d = d;
            this.f = f;
        }
    }

    /**
     * A Data Transfer Object (DTO) representing wrapped data types (e.g., Double, Float).
     */
    static class WrapDTO extends SelfDescribingMarshallable {
        Double d;
        Float f;

        WrapDTO(Double d, Float f) {
            this.d = d;
            this.f = f;
        }
    }

    /**
     * A Data Transfer Object (DTO) representing general objects.
     * It can hold various object types, including Double and Float wrappers.
     */
    static class ObjectWrapDTO extends SelfDescribingMarshallable {
        Object d;
        Object f;

        ObjectWrapDTO(Object d, Object f) {
            this.d = d;
            this.f = f;
        }
    }
}
