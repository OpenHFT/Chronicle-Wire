/*
 * Copyright 2016-2026 chronicle.software
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Covers binary round-tripping of short UTF-8 strings whose encoded byte length
 * is greater than their UTF-16 character count.
 */
public class BinaryMultiByteStringTruncationTest extends WireTestCommon {

    private static final String MULTI_BYTE = "\u221A";

    private static <T> T roundTrip(WireType wireType, Class<T> type, T value) {
        return roundTrip(Bytes.allocateElasticOnHeap(), wireType, type, value);
    }

    private static <T> T roundTrip(Bytes<?> bytes, WireType wireType, Class<T> type, T value) {
        try {
            Wire wire = wireType.apply(bytes);
            wire.getValueOut().object(type, value);
            return wire.getValueIn().object(type);
        } finally {
            bytes.releaseLast();
        }
    }

    private static Dto dto(String first) {
        Dto d = new Dto();
        d.m = first;
        d.f1 = "1";
        d.f2 = "2";
        d.f3 = "3";
        d.f4 = "4";
        d.f5 = "5";
        return d;
    }

    @Test
    public void binaryRoundTripsAllFieldsAfterMultiByteString() {
        assertDtoRoundTrip(WireType.BINARY, MULTI_BYTE);
    }

    @Test
    public void binaryRoundTripsLongFieldNameAfterMultiByteString() {
        LongFieldDto d = new LongFieldDto();
        d.m = MULTI_BYTE;
        d.thisFieldNameIsLongEnoughToUseFieldNameAny = "long";
        LongFieldDto back = roundTrip(WireType.BINARY, LongFieldDto.class, d);
        assertEquals(MULTI_BYTE, back.m);
        assertEquals("long", back.thisFieldNameIsLongEnoughToUseFieldNameAny);
    }

    @Test
    public void binaryRoundTripsAllFieldsAfterMultiByteStringDirectMemory() {
        // Chronicle Queue reads memory-mapped (direct / NativeBytesStore) bytes, which take a
        // different field-name read path (parse8bit_SB1) than on-heap bytes. That path was never
        // broken; this guards it stays correct after a multi-byte field if the read paths are reworked.
        Dto back = roundTrip(Bytes.allocateElasticDirect(), WireType.BINARY, Dto.class, dto(MULTI_BYTE));
        assertAllFields(back, MULTI_BYTE);
    }

    @Test
    public void binaryRoundTripsAllFieldsForAsciiString() {
        assertDtoRoundTrip(WireType.BINARY, "ascii");
    }

    @Test
    public void textRoundTripsAllFieldsForMultiByteString() {
        assertDtoRoundTrip(WireType.TEXT, MULTI_BYTE);
    }

    private static void assertDtoRoundTrip(WireType wireType, String first) {
        assertAllFields(roundTrip(wireType, Dto.class, dto(first)), first);
    }

    private static void assertAllFields(Dto back, String first) {
        assertEquals(first, back.m);
        assertEquals("1", back.f1);
        assertEquals("2", back.f2);
        assertEquals("3", back.f3);
        assertEquals("4", back.f4);
        assertEquals("5", back.f5);
    }

    public static class LongFieldDto extends SelfDescribingMarshallable {
        String m;
        String thisFieldNameIsLongEnoughToUseFieldNameAny;
    }

    public static class Dto extends SelfDescribingMarshallable {
        String m;
        String f1;
        String f2;
        String f3;
        String f4;
        String f5;
    }
}
