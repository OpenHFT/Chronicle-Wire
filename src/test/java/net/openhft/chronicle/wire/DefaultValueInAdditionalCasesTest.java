/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.PointerBytesStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers DefaultValueIn branches for bytes, text, and primitive defaults in TextWire.
 */
class DefaultValueInAdditionalCasesTest extends WireTestCommon {

    @Test
    @DisplayName("Handles bytes text and match branches safely")
    void bytesTextAndMatchBranches() {
        TextWire tw = TextWire.from("");
        DefaultValueIn dvi = new DefaultValueIn(tw);

        // bytes/text from non-null default
        Bytes<?> src = Bytes.wrapForRead("hello".getBytes(StandardCharsets.ISO_8859_1));
        dvi.defaultValue = src;
        Bytes<?> out = Bytes.allocateElasticOnHeap(16);
        assertSame(out, dvi.textTo(out),
                "textTo should return provided buffer when default is bytes");
        assertEquals("hello", out.toString(),
                "textTo should copy default bytes into output string");

        final boolean[] match = {false};
        dvi.bytesMatch(src, b -> match[0] = b);
        assertTrue(match[0], "bytesMatch should report true for identical bytes");

        // bytesSet with null default (non-null requires direct memory address)
        PointerBytesStore pbs = new PointerBytesStore();
        dvi.defaultValue = null;
        assertSame(dvi.wireIn(), dvi.bytesSet(pbs),
                "bytesSet should return wireIn when default is null");
    }

    @Test
    @DisplayName("Uses zero defaults when primitive default is null")
    void primitiveDefaultsAreZeroWhenNull() {
        TextWire tw = TextWire.from("");
        DefaultValueIn dvi = new DefaultValueIn(tw);
        dvi.defaultValue = null;

        final int[] gotI = {1};
        dvi.int32(gotI, (arr, v) -> arr[0] = v);
        assertEquals(0, gotI[0], "int32 default should be zero when defaultValue is null");

        final long[] gotL = {1};
        dvi.int64(gotL, (arr, v) -> arr[0] = v);
        assertEquals(0L, gotL[0], "int64 default should be zero when defaultValue is null");

        final float[] gotF = {1f};
        dvi.float32(gotF, (arr, v) -> arr[0] = v);
        assertEquals(0f, gotF[0], 0f, "float32 default should be zero when defaultValue is null");
    }

    @Test
    @DisplayName("Reads bytes array default into target buffer")
    void bytesArrayAccessor() {
        TextWire tw = TextWire.from("");
        DefaultValueIn dvi = new DefaultValueIn(tw);
        byte[] data = {1, 2, 3};
        dvi.defaultValue = data;
        byte[] using = new byte[3];
        assertArrayEquals(data, dvi.bytes(using),
                "bytes accessor should return default byte array");
    }

    // ========== Text Methods Tests ==========

    @Test
    @DisplayName("DefaultValueIn.text returns null for null defaultValue in WireIn")
    void textReturnsNullForNullDefault() {
        TextWire tw = TextWire.from("");
        DefaultValueIn dvi = new DefaultValueIn(tw);
        dvi.defaultValue = null;
        assertNull(dvi.text(), "DefaultValueIn.text should return null when defaultValue is null");
    }

    @Test
    @DisplayName("DefaultValueIn.text returns toString for non-null default")
    void textReturnsToStringOfDefault() {
        TextWire tw = TextWire.from("");
        DefaultValueIn dvi = new DefaultValueIn(tw);
        dvi.defaultValue = "hello";
        assertEquals("hello", dvi.text(), "DefaultValueIn.text should return String default unchanged");

        dvi.defaultValue = 42;
        assertEquals("42", dvi.text(), "DefaultValueIn.text should return numeric default via toString");
    }

    @Test
    @DisplayName("DefaultValueIn.textTo returns null for null defaultValue StringBuilder")
    void textToStringBuilderNullDefault() {
        TextWire tw = TextWire.from("");
        DefaultValueIn dvi = new DefaultValueIn(tw);
        dvi.defaultValue = null;
        StringBuilder sb = new StringBuilder();
        assertNull(dvi.textTo(sb), "DefaultValueIn.textTo should return null when defaultValue is null");
    }

    @Test
    @DisplayName("DefaultValueIn.textTo appends defaultValue into StringBuilder")
    void textToStringBuilderNonNull() {
        TextWire tw = TextWire.from("");
        DefaultValueIn dvi = new DefaultValueIn(tw);
        dvi.defaultValue = "world";
        StringBuilder sb = new StringBuilder();
        assertSame(sb, dvi.textTo(sb), "DefaultValueIn.textTo should return provided StringBuilder");
        assertEquals("world", sb.toString(), "DefaultValueIn.textTo should append defaultValue content");
    }

    // ========== Numeric Type Methods Tests ==========

    @Test
    @DisplayName("int8 methods should handle null and numeric defaults")
    void int8MethodsHandleNullAndNumeric() {
        TextWire tw = TextWire.from("");
        DefaultValueIn dvi = new DefaultValueIn(tw);

        dvi.defaultValue = null;
        assertEquals((byte) 0, dvi.int8(), "int8() should return 0 for null default");

        dvi.defaultValue = (byte) 42;
        assertEquals((byte) 42, dvi.int8(), "int8() should return byte default value");

        final byte[] gotB = {1};
        dvi.int8(gotB, (arr, v) -> arr[0] = v);
        assertEquals((byte) 42, gotB[0], "int8 consumer should receive default byte value");
    }

    @Test
    @DisplayName("uint8 methods should handle null and numeric defaults")
    void uint8MethodsHandleNullAndNumeric() {
        TextWire tw = TextWire.from("");
        DefaultValueIn dvi = new DefaultValueIn(tw);

        dvi.defaultValue = null;
        final short[] got = {1};
        dvi.uint8(got, (arr, v) -> arr[0] = v);
        assertEquals((short) 0, got[0], "uint8 consumer should receive 0 for null default");

        dvi.defaultValue = (short) 200;
        dvi.uint8(got, (arr, v) -> arr[0] = v);
        assertEquals((short) 200, got[0], "uint8 consumer should receive default short value");
    }

    @Test
    @DisplayName("int16 and uint16 methods should handle defaults")
    void int16Uint16MethodsHandleDefaults() {
        TextWire tw = TextWire.from("");
        DefaultValueIn dvi = new DefaultValueIn(tw);

        dvi.defaultValue = null;
        assertEquals((short) 0, dvi.int16(), "int16() should return 0 for null default");
        assertEquals(0, dvi.uint16(), "uint16() should return 0 for null default");

        dvi.defaultValue = (short) 12345;
        assertEquals((short) 12345, dvi.int16(), "int16() should return short default value");

        final int[] gotU = {1};
        dvi.uint16(gotU, (arr, v) -> arr[0] = v);
        assertEquals(12345, gotU[0], "uint16 consumer should receive default int value");
    }

    @Test
    @DisplayName("DefaultValueIn.uint32 consumer handles null and numeric defaults")
    void uint32MethodHandleDefaults() {
        TextWire tw = TextWire.from("");
        DefaultValueIn dvi = new DefaultValueIn(tw);

        dvi.defaultValue = null;
        final long[] got = {1L};
        dvi.uint32(got, (arr, v) -> arr[0] = v);
        assertEquals(0L, got[0], "DefaultValueIn.uint32 should provide 0L for null default");

        dvi.defaultValue = 100000L;
        dvi.uint32(got, (arr, v) -> arr[0] = v);
        assertEquals(100000L, got[0], "DefaultValueIn.uint32 should provide long default value");
    }

    @Test
    @DisplayName("DefaultValueIn.float32 returns correct default values")
    void float32MethodReturnDefault() {
        TextWire tw = TextWire.from("");
        DefaultValueIn dvi = new DefaultValueIn(tw);

        dvi.defaultValue = null;
        assertEquals(0f, dvi.float32(), 0f, "DefaultValueIn.float32 should return 0f for null default");

        dvi.defaultValue = 3.14f;
        assertEquals(3.14f, dvi.float32(), 0.001f, "DefaultValueIn.float32 should return float default value");
    }

    // ========== Boolean Methods Tests ==========

    @Test
    @DisplayName("DefaultValueIn.bool handles true, false, and null defaults")
    void boolHandlesVariousDefaults() {
        TextWire tw = TextWire.from("");
        DefaultValueIn dvi = new DefaultValueIn(tw);

        dvi.defaultValue = Boolean.TRUE;
        assertTrue(dvi.bool(), "DefaultValueIn.bool should return true for Boolean.TRUE");

        dvi.defaultValue = Boolean.FALSE;
        assertFalse(dvi.bool(), "DefaultValueIn.bool should return false for Boolean.FALSE");

        dvi.defaultValue = null;
        assertFalse(dvi.bool(), "DefaultValueIn.bool should return false for null default");

        dvi.defaultValue = "notBoolean";
        assertFalse(dvi.bool(), "DefaultValueIn.bool should return false for non-Boolean default");
    }

    // ========== Type Prefix Methods Tests ==========

    @Test
    @DisplayName("DefaultValueIn.typePrefix returns void.class for null default")
    void typePrefixReturnsVoidForNull() {
        TextWire tw = TextWire.from("");
        DefaultValueIn dvi = new DefaultValueIn(tw);
        dvi.defaultValue = null;
        assertEquals(void.class, dvi.typePrefix(), "DefaultValueIn.typePrefix should return void.class for null default");
    }

    @Test
    @DisplayName("DefaultValueIn.typePrefix returns value class for non-null defaultValue type")
    void typePrefixReturnsClassOfDefault() {
        TextWire tw = TextWire.from("");
        DefaultValueIn dvi = new DefaultValueIn(tw);
        dvi.defaultValue = "hello";
        assertEquals(String.class, dvi.typePrefix(), "DefaultValueIn.typePrefix should return String.class");

        dvi.defaultValue = 42;
        assertEquals(Integer.class, dvi.typePrefix(), "DefaultValueIn.typePrefix should return Integer.class");
    }

    @Test
    @DisplayName("typePrefix consumer should receive null for defaults")
    void typePrefixConsumerReceivesNull() {
        TextWire tw = TextWire.from("");
        DefaultValueIn dvi = new DefaultValueIn(tw);
        dvi.defaultValue = "test";

        final String[] got = {""};
        dvi.typePrefix(got, (arr, cs) -> arr[0] = cs != null ? cs.toString() : null);
        assertNull(got[0], "DefaultValueIn.typePrefix should provide null to CharSequence consumer");
    }

    // ========== State Query Methods Tests ==========

    @Test
    @DisplayName("DefaultValueIn.isNull reports true only for null defaultValue")
    void isNullReturnsCorrectly() {
        TextWire tw = TextWire.from("");
        DefaultValueIn dvi = new DefaultValueIn(tw);

        dvi.defaultValue = null;
        assertTrue(dvi.isNull(), "DefaultValueIn.isNull should return true for null default");

        dvi.defaultValue = "value";
        assertFalse(dvi.isNull(), "DefaultValueIn.isNull should return false for non-null default");
    }

    @Test
    @DisplayName("DefaultValueIn.isPresent returns false for all default states")
    void isPresentAlwaysFalse() {
        TextWire tw = TextWire.from("");
        DefaultValueIn dvi = new DefaultValueIn(tw);

        dvi.defaultValue = null;
        assertFalse(dvi.isPresent(), "DefaultValueIn.isPresent should return false for null default");

        dvi.defaultValue = "value";
        assertFalse(dvi.isPresent(), "DefaultValueIn.isPresent should return false for non-null default");
    }

    @Test
    @DisplayName("DefaultValueIn.isTyped returns false for all default states")
    void isTypedAlwaysFalse() {
        TextWire tw = TextWire.from("");
        DefaultValueIn dvi = new DefaultValueIn(tw);

        dvi.defaultValue = null;
        assertFalse(dvi.isTyped(), "DefaultValueIn.isTyped should return false for null default");

        dvi.defaultValue = "value";
        assertFalse(dvi.isTyped(), "DefaultValueIn.isTyped should return false for non-null default");
    }

    // ========== Sequence Methods Tests ==========

    @Test
    @DisplayName("DefaultValueIn.hasNext returns false for default values")
    void hasNextAlwaysFalse() {
        TextWire tw = TextWire.from("");
        DefaultValueIn dvi = new DefaultValueIn(tw);
        dvi.defaultValue = null;
        assertFalse(dvi.hasNext(), "DefaultValueIn.hasNext should return false for default values");
    }

    @Test
    @DisplayName("DefaultValueIn.hasNextSequenceItem returns false for default state")
    void hasNextSequenceItemAlwaysFalse() {
        TextWire tw = TextWire.from("");
        DefaultValueIn dvi = new DefaultValueIn(tw);
        dvi.defaultValue = null;
        assertFalse(dvi.hasNextSequenceItem(), "DefaultValueIn.hasNextSequenceItem should return false for defaults");
    }

    @Test
    @DisplayName("DefaultValueIn.sequence returns false for default values")
    void sequenceBiConsumerReturnsFalse() {
        TextWire tw = TextWire.from("");
        DefaultValueIn dvi = new DefaultValueIn(tw);
        dvi.defaultValue = null;

        boolean result = dvi.sequence("holder", (h, v) -> {
            // Should not be called
        });
        assertFalse(result, "DefaultValueIn.sequence should return false for default values");
    }

    // ========== Other Methods Tests ==========

    @Test
    @DisplayName("readLength() should always return 0 for defaults")
    void readLengthReturnsZero() {
        TextWire tw = TextWire.from("");
        DefaultValueIn dvi = new DefaultValueIn(tw);
        dvi.defaultValue = null;
        assertEquals(0L, dvi.readLength(), "readLength() should return 0 for defaults");
    }

    @Test
    @DisplayName("DefaultValueIn.skipValue returns the original WireIn")
    void skipValueReturnsWireIn() {
        TextWire tw = TextWire.from("");
        DefaultValueIn dvi = new DefaultValueIn(tw);
        assertSame(tw, dvi.skipValue(), "DefaultValueIn.skipValue should return the original WireIn");
    }

    @Test
    @DisplayName("DefaultValueIn.getBracketType returns NONE for defaults")
    void getBracketTypeReturnsNone() {
        TextWire tw = TextWire.from("");
        DefaultValueIn dvi = new DefaultValueIn(tw);
        assertEquals(BracketType.NONE, dvi.getBracketType(), "DefaultValueIn.getBracketType should return NONE for defaults");
    }

    @Test
    @DisplayName("DefaultValueIn.typedMarshallable returns the default object value")
    void typedMarshallableReturnsDefault() {
        TextWire tw = TextWire.from("");
        DefaultValueIn dvi = new DefaultValueIn(tw);

        dvi.defaultValue = null;
        assertNull(dvi.typedMarshallable(), "DefaultValueIn.typedMarshallable should return null default");

        dvi.defaultValue = "test";
        assertEquals("test", dvi.typedMarshallable(), "DefaultValueIn.typedMarshallable should return default value");
    }

    @Test
    @DisplayName("DefaultValueIn.typeLiteral returns defaultValue as Type")
    void typeLiteralReturnsDefault() {
        TextWire tw = TextWire.from("");
        DefaultValueIn dvi = new DefaultValueIn(tw);

        dvi.defaultValue = String.class;
        assertEquals(String.class, dvi.typeLiteral((cs, ex) -> null),
                "DefaultValueIn.typeLiteral should return Type default");

        dvi.defaultValue = null;
        assertNull(dvi.typeLiteral((cs, ex) -> null),
                "DefaultValueIn.typeLiteral should return null for null default");
    }

    @Test
    @DisplayName("DefaultValueIn.objectWithInferredType returns the default object value")
    void objectWithInferredTypeReturnsDefault() {
        TextWire tw = TextWire.from("");
        DefaultValueIn dvi = new DefaultValueIn(tw);

        dvi.defaultValue = "test";
        assertEquals("test", dvi.objectWithInferredType(null, null, String.class),
                "DefaultValueIn.objectWithInferredType should return default value");
    }

    @Test
    @DisplayName("DefaultValueIn.marshallable returns the default value unchanged")
    void marshallableReturnsDefault() {
        TextWire tw = TextWire.from("");
        DefaultValueIn dvi = new DefaultValueIn(tw);

        dvi.defaultValue = "test";
        assertEquals("test", dvi.marshallable(new Object(), SerializationStrategies.ANY_OBJECT),
                "DefaultValueIn.marshallable should return default value unchanged");
    }

    @Test
    @DisplayName("DefaultValueIn.bytesMatch reports false for non-BytesStore defaults")
    void bytesMatchFalseForNonBytesStore() {
        TextWire tw = TextWire.from("");
        DefaultValueIn dvi = new DefaultValueIn(tw);
        dvi.defaultValue = "not bytes";

        final boolean[] match = {true};
        Bytes<?> compare = Bytes.wrapForRead("test".getBytes(StandardCharsets.ISO_8859_1));
        dvi.bytesMatch(compare, b -> match[0] = b);
        assertFalse(match[0], "DefaultValueIn.bytesMatch should report false for non-BytesStore default");
    }

    @Test
    @DisplayName("DefaultValueIn.resetState call completes without exceptions")
    void resetStateDoesNotThrow() {
        TextWire tw = TextWire.from("");
        DefaultValueIn dvi = new DefaultValueIn(tw);
        dvi.resetState();  // Should not throw
        assertNotNull(dvi, "DefaultValueIn.resetState should complete without exception");
    }
}
