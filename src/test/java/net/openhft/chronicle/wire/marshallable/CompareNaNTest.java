/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.WireTestCommon;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

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
    @DisplayName("NaN primitives should compare equal in DTO")
    public void testPrim() {
        @NotNull PrimDTO a = new PrimDTO(Double.NaN, Float.NaN);
        @NotNull PrimDTO b = new PrimDTO(Double.NaN, Float.NaN);
        assertTrue(Double.isNaN(a.d), "PrimDTO.d should be NaN for double input");
        assertTrue(Float.isNaN(a.f), "PrimDTO.f should be NaN for float input");
        assertEquals(a.toString(), b.toString(), "Rendered text should match for NaN primitives");
        assertEquals(a, b, "DTOs with NaN primitives should compare equal");
    }

    /**
     * Test the comparison behavior for wrapper data types containing NaN values.
     * Ensures that two DTOs with NaN wrapped values are considered equal.
     */
    @Test
    @DisplayName("NaN wrapper values should compare equal in DTO")
    public void testWrapDTO() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory must be available for NaN wrapper DTO test");

        @NotNull WrapDTO a = new WrapDTO(Double.NaN, Float.NaN);
        @NotNull WrapDTO b = new WrapDTO(Double.NaN, Float.NaN);
        assertTrue(Double.isNaN(a.d), "WrapDTO.d should be NaN for Double input");
        assertTrue(Float.isNaN(a.f), "WrapDTO.f should be NaN for Float input");
        assertEquals(a.toString(), b.toString(), "Rendered text should match for NaN wrappers");
        assertEquals(a, b, "DTOs with NaN wrappers should compare equal");
    }

    /**
     * Test the comparison behavior for objects containing NaN values.
     * Ensures that two DTOs with NaN object values are considered equal.
     */
    @Test
    @DisplayName("NaN object values should compare equal in DTO")
    public void testObjectWrapDTO() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory must be available for NaN object DTO test");

        @NotNull ObjectWrapDTO a = new ObjectWrapDTO(Double.NaN, Float.NaN);
        @NotNull ObjectWrapDTO b = new ObjectWrapDTO(Double.NaN, Float.NaN);
        assertTrue(Double.isNaN((Double) a.d), "ObjectWrapDTO.d should be NaN for Double input");
        assertTrue(Float.isNaN((Float) a.f), "ObjectWrapDTO.f should be NaN for Float input");
        assertEquals(a.toString(), b.toString(), "Rendered text should match for NaN objects");
        assertEquals(a, b, "DTOs with NaN objects should compare equal");
    }

    /**
     * A Data Transfer Object (DTO) representing primitive data types.
     */
    static class PrimDTO extends SelfDescribingMarshallable {
        final double d;
        final float f;

        PrimDTO(double d, float f) {
            this.d = d;
            this.f = f;
        }
    }

    /**
     * A Data Transfer Object (DTO) representing wrapped data types (e.g., Double, Float).
     */
    static class WrapDTO extends SelfDescribingMarshallable {
        final Double d;
        final Float f;

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
        final Object d;
        final Object f;

        ObjectWrapDTO(Object d, Object f) {
            this.d = d;
            this.f = f;
        }
    }
}
