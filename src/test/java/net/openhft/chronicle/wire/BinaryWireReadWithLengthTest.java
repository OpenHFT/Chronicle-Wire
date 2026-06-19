/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.util.DecoratedBufferUnderflowException;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import org.junit.Test;

import java.nio.BufferUnderflowException;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;

/**
 * Illustrates Chronicle-Queue style copying of binary fragments into a textual representation.
 */
public class BinaryWireReadWithLengthTest extends WireTestCommon {

    @Test
    public void copiesMapFragmentToText() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire writer = new BinaryWire(bytes);
        try (DocumentContext dc = writer.writingDocument(false)) {
            dc.wire().write("map").marshallable(m -> m.write("key").int32(1));
        }

        bytes.readPositionRemaining(0, bytes.writePosition());
        int header = bytes.readInt();
        int len = Wires.lengthOf(header);
        long bodyPos = bytes.readPosition();

        TextWire target = new TextWire(Bytes.allocateElasticOnHeap());
        BinaryWire source = new BinaryWire(bytes);
        source.bytes().readPosition(bodyPos);
        source.readWithLength(target, len);

        assertTrue(target.bytes().toString().contains("key: 1"));
    }

    @Test
    public void copiesSequenceFragmentToText() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire writer = new BinaryWire(bytes);
        try (DocumentContext dc = writer.writingDocument(false)) {
            dc.wire().write("seq").sequence(v -> {
                v.text("first");
                v.int32(2);
            });
        }

        bytes.readPositionRemaining(0, bytes.writePosition());
        int header = bytes.readInt();
        int len = Wires.lengthOf(header);
        long bodyPos = bytes.readPosition();

        TextWire target = new TextWire(Bytes.allocateElasticOnHeap());
        BinaryWire source = new BinaryWire(bytes);
        source.bytes().readPosition(bodyPos);
        source.readWithLength(target, len);

        String dump = target.bytes().toString();
        assertTrue(dump.contains("first"));
        assertTrue(dump.contains("2"));
    }

    @Test
    public void copyEntireWireToText() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire writer = new BinaryWire(bytes);
        writer.writeEventName("say").text("hello");
        writer.writeEventName("number").int32(42);

        bytes.readPositionRemaining(0, bytes.writePosition());
        Wire textWire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());
        new BinaryWire(bytes).copyTo(textWire);
        String output = textWire.bytes().toString();

        assertTrue(output.contains("say: hello"));
        assertTrue(output.contains("number: 42"));
    }

    @Test
    public void readWithLengthLongOverloadCopiesMapFragmentToText() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire writer = new BinaryWire(bytes);
        try (DocumentContext dc = writer.writingDocument(false)) {
            dc.wire().write("map").marshallable(m -> m.write("key").int32(1));
        }

        bytes.readPositionRemaining(0, bytes.writePosition());
        int header = bytes.readInt();
        long len = Wires.lengthOf(header);
        long bodyPos = bytes.readPosition();

        TextWire target = new TextWire(Bytes.allocateElasticOnHeap());
        BinaryWire source = new BinaryWire(bytes);
        source.bytes().readPosition(bodyPos);
        source.readWithLength(target, len);

        assertTrue(target.bytes().toString().contains("key: 1"));
    }

    @Test(expected = net.openhft.chronicle.core.io.IORuntimeException.class)
    public void readWithLengthRejectsNegativeLength() {
        BinaryWire source = new BinaryWire(Bytes.allocateElasticOnHeap());
        source.readWithLength(new TextWire(Bytes.allocateElasticOnHeap()), -1);
    }

    @Test(expected = net.openhft.chronicle.core.io.IORuntimeException.class)
    public void readWithLengthRejectsOver32BitLength() {
        BinaryWire source = new BinaryWire(Bytes.allocateElasticOnHeap());
        source.readWithLength(new TextWire(Bytes.allocateElasticOnHeap()), 1L << 32);
    }

    @Test
    public void bytesLength32WithTopBitSetIsReadAsUnsigned() {
        expectUnsignedLengthRejected(0x80000000L);
    }

    @Test
    public void bytesLength32WithMaxUnsignedValueIsReadAsUnsigned() {
        expectUnsignedLengthRejected(0xFFFFFFFFL);
    }

    @Test
    public void uncheckedBytesLength32WithMaxUnsignedValueIsRejectedBeforeReadLimitIsExtended() {
        Bytes<?> uncheckedBytes = uncheckedBytesLength32(0xFFFFFFFFL);
        try {
            BinaryWire source = new BinaryWire(uncheckedBytes);
            TextWire target = new TextWire(Bytes.allocateElasticOnHeap());
            try {
                source.copyOne(target);
                fail("Expected IORuntimeException for unchecked unsigned 32-bit length");
            } catch (IORuntimeException e) {
                assertTrue("Unexpected message: " + e.getMessage(),
                        e.getMessage().contains("bytes remaining between readPosition"));
            }
        } finally {
            uncheckedBytes.releaseLast();
        }
    }

    @Test
    public void uncheckedObjectWithLength32BeyondHardLimitIsRejectedBeforeReadLimitIsExtended()
            throws InvalidMarshallableException {
        Bytes<?> uncheckedBytes = uncheckedBytesLength32(0xFFFFFFFFL, BinaryWireCode.EVENT_NAME);
        try {
            BinaryWire source = new BinaryWire(uncheckedBytes);
            try {
                source.getValueIn().object();
                fail("Expected IORuntimeException for unchecked unsigned 32-bit object length");
            } catch (IORuntimeException e) {
                assertTrue("Unexpected message: " + e.getMessage(),
                        e.getMessage().contains("bytes remaining between readPosition"));
            }
        } finally {
            uncheckedBytes.releaseLast();
        }
    }

    @Test
    public void valueInBytesRejectsMaxUnsignedLengthWithScalarPayload() {
        expectValueInBytesRejectsMaxUnsignedLength(BinaryWireCode.TRUE, 0x55);
    }

    @Test
    public void valueInBytesRejectsMaxUnsignedLengthWithU8ArrayPayload() {
        expectValueInBytesRejectsMaxUnsignedLength(BinaryWireCode.U8_ARRAY, 0x55);
    }

    @Test
    public void objectRejectsMaxUnsignedLengthWithoutReadingNestedU8Array() {
        Bytes<?> bytes = bytesLength32(0xFFFFFFFFL, BinaryWireCode.U8_ARRAY, 1, 2, 3);
        try {
            BinaryWire source = new BinaryWire(bytes);
            try {
                source.getValueIn().object();
                fail("Expected IORuntimeException for unsigned 32-bit object length");
            } catch (BufferUnderflowException expected) {
            }
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    public void valueInBytesCopiesValidBytesLength32U8ArrayPayload() {
        Bytes<?> bytes = bytesLength32(4, BinaryWireCode.U8_ARRAY, 1, 2, 3);
        Bytes<?> out = Bytes.allocateElasticOnHeap();
        try {
            new BinaryWire(bytes).getValueIn().bytes(out);

            assertEquals(3, out.writePosition());
            assertArrayEquals(new byte[]{1, 2, 3}, out.toByteArray());
            assertEquals(9, bytes.readPosition());
            assertTrue(bytes.readPosition() <= bytes.readLimit());
        } finally {
            out.releaseLast();
            bytes.releaseLast();
        }
    }

    private void expectValueInBytesRejectsMaxUnsignedLength(int... payloadCodes) {
        Bytes<?> bytes = bytesLength32(0xFFFFFFFFL, payloadCodes);
        Bytes<?> out = Bytes.allocateElasticOnHeap();
        out.writeByte((byte) 0x66);
        try {
            BinaryWire source = new BinaryWire(bytes);
            try {
                source.getValueIn().bytes(out);
            } catch (BufferUnderflowException e) {
                // expected
            }
        } finally {
            out.releaseLast();
            bytes.releaseLast();
        }
    }

    private Bytes<?> uncheckedBytesLength32(long length, int... payloadCodes) {
        return bytesLength32(length, payloadCodes).unchecked(true);
    }

    private Bytes<?> bytesLength32(long length, int... payloadCodes) {
        Bytes<?> encoded = Bytes.allocateElasticOnHeap();
        encoded.writeUnsignedByte(BinaryWireCode.BYTES_LENGTH32);
        encoded.writeUnsignedInt(length);
        for (int payloadCode : payloadCodes)
            encoded.writeUnsignedByte(payloadCode);
        byte[] data = encoded.toByteArray();
        encoded.releaseLast();
        return Bytes.wrapForRead(data);
    }

    /**
     * Builds a stream of BYTES_LENGTH32 followed by a 4-byte length with the top bit set,
     * but no payload. The length must be read as unsigned 32-bit, so the (impossibly large)
     * length fails the read-limit check, reporting the bytes remaining. Before the fix,
     * {@code copyOne} read the length with the signed {@code readInt()}, so the limit check
     * passed on a negative length and reading continued with a corrupt read limit.
     */
    private void expectUnsignedLengthRejected(long length) {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        bytes.writeUnsignedByte(BinaryWireCode.BYTES_LENGTH32);
        bytes.writeUnsignedInt(length);
        bytes.readPositionRemaining(0, bytes.writePosition());

        BinaryWire source = new BinaryWire(bytes);
        TextWire target = new TextWire(Bytes.allocateElasticOnHeap());
        try {
            source.copyOne(target);
            fail("Expected IORuntimeException for unsigned 32-bit length " + length);
        } catch (IORuntimeException e) {
            assertTrue("Unexpected message: " + e.getMessage(),
                    e.getMessage().contains("bytes remaining between readPosition"));
        }
    }

    @Test
    public void copyMessagesIndividually() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire writer = new BinaryWire(bytes);
        writer.writeEventName("alpha").text("one");
        writer.writeEventName("beta").int32(2);

        bytes.readPositionRemaining(0, bytes.writePosition());
        Wire textWire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());
        BinaryWire source = new BinaryWire(bytes);

        source.copyOne(textWire);
        String first = textWire.bytes().toString();
        assertTrue(first.length() > 0);
    }

    @Test
    public void valueInBytesRejectsTypePrefixThatExceedsDeclaredLength() {
        byte[] payload = {
                (byte) BinaryWireCode.BYTES_LENGTH8,
                0x01,
                (byte) BinaryWireCode.TYPE_PREFIX,
                0x03,
                'x', 'x', 'x',
                (byte) BinaryWireCode.U8_ARRAY,
                0x55
        };
        Bytes<?> bytes = Bytes.wrapForRead(payload).unchecked(true);

        long originalLimit = bytes.readLimit();
        BinaryWire wire = new BinaryWire(bytes);
        Bytes<?> output = Bytes.allocateElasticOnHeap();
        output.writeByte((byte) 0x66);

        RuntimeException e = assertThrows(RuntimeException.class,
                () -> wire.getValueIn().bytes(output));

        assertNotNull(e);
        assertEquals(0, output.writePosition());
        assertEquals(originalLimit, bytes.readLimit());
        assertTrailingTypePrefixBytes(bytes);
    }

    @Test
    public void readTextRejectsTruncatedPadding32LengthField() {
        byte[] payload = {
                0x04, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x02
        };
        Bytes<?> bytes1 = Bytes.wrapForRead(payload).unchecked(true);
        bytes1.readLimit(4);

        BinaryWire wire = new BinaryWire(bytes1);
        StringBuilder sb = new StringBuilder();

        DecoratedBufferUnderflowException e = assertThrows(DecoratedBufferUnderflowException.class,
                () -> wire.readText(BinaryWireCode.PADDING32, sb));

        assertTrue(e.getMessage().contains("Requires at least five bytes, readRemaining: "));
        assertEquals(0, bytes1.readPosition());
        assertEquals(4, bytes1.readLimit());
        assertEquals(4, bytes1.readRemaining());
        assertTrue(bytes1.readPosition() <= bytes1.readLimit());
    }

    @Test
    public void valueInBytesRejectsPadding32ThatExceedsDeclaredLength() {
        byte[] payload = {
                (byte) BinaryWireCode.BYTES_LENGTH8,
                0x01,
                (byte) BinaryWireCode.PADDING32,
                0x04, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00,
                (byte) BinaryWireCode.TRUE
        };
        Bytes<?> bytes = Bytes.wrapForRead(payload).unchecked(true);

        long originalLimit = bytes.readLimit();
        BinaryWire wire = new BinaryWire(bytes);
        Bytes<?> output = Bytes.allocateElasticOnHeap();
        output.writeByte((byte) 0x66);

        DecoratedBufferUnderflowException e = assertThrows(DecoratedBufferUnderflowException.class,
                () -> wire.getValueIn().bytes(output));

        assertTrue(e.getMessage().contains("Requires at least five bytes, readRemaining: "));
        assertEquals(0, output.writePosition());
        assertEquals(originalLimit, bytes.readLimit());
        assertTrailingPaddingAndTrue(bytes);
    }

    @Test
    public void byteArrayReturnsEmptyArrayForZeroLengthBody() {
        byte[] payload = {
                (byte) BinaryWireCode.BYTES_LENGTH32,
                0x00, 0x00, 0x00, 0x00,
                (byte) BinaryWireCode.U8_ARRAY,
                0x55
        };
        Bytes<?> bytes2 = Bytes.wrapForRead(payload).unchecked(true);
        assertEquals(0, bytes2.readInt(1));
        bytes2.readLimit(5);

        BinaryWire wire = new BinaryWire(bytes2);
        byte[] using = {(byte) 0x66};

        byte[] bytes1 = wire.getValueIn().bytes(using);

        assertEquals(0, bytes1.length);
    }

    static void assertTrailingPaddingAndTrue(Bytes<?> bytes) {
        assertEquals(BinaryWireCode.PADDING32, bytes.readUnsignedByte(2));
        assertEquals(0x04, bytes.readUnsignedByte(3));
        assertEquals(0x00, bytes.readUnsignedByte(4));
        assertEquals(0x00, bytes.readUnsignedByte(5));
        assertEquals(0x00, bytes.readUnsignedByte(6));
        assertEquals(0x00, bytes.readUnsignedByte(7));
        assertEquals(0x00, bytes.readUnsignedByte(8));
        assertEquals(0x00, bytes.readUnsignedByte(9));
        assertEquals(0x00, bytes.readUnsignedByte(10));
        assertEquals(BinaryWireCode.TRUE, bytes.readUnsignedByte(11));
    }

    static void assertTrailingTypePrefixBytes(Bytes<?> bytes) {
        assertEquals(BinaryWireCode.TYPE_PREFIX, bytes.readUnsignedByte(2));
        assertEquals(0x03, bytes.readUnsignedByte(3));
        assertEquals('x', bytes.readUnsignedByte(4));
        assertEquals('x', bytes.readUnsignedByte(5));
        assertEquals('x', bytes.readUnsignedByte(6));
        assertEquals(BinaryWireCode.U8_ARRAY, bytes.readUnsignedByte(7));
        assertEquals(0x55, bytes.readUnsignedByte(8));
    }
}
