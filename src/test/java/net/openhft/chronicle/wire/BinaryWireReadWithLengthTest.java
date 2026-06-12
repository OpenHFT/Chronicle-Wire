/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
                        e.getMessage().contains("Can't extend the limit"));
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
                        e.getMessage().contains("Can't extend the limit"));
            }
        } finally {
            uncheckedBytes.releaseLast();
        }
    }

    private Bytes<?> uncheckedBytesLength32(long length, int... payloadCodes) {
        Bytes<?> encoded = Bytes.allocateElasticOnHeap();
        encoded.writeUnsignedByte(BinaryWireCode.BYTES_LENGTH32);
        encoded.writeUnsignedInt(length);
        for (int payloadCode : payloadCodes)
            encoded.writeUnsignedByte(payloadCode);
        byte[] data = encoded.toByteArray();
        encoded.releaseLast();
        return Bytes.wrapForRead(data).unchecked(true);
    }

    /**
     * Builds a stream of BYTES_LENGTH32 followed by a 4-byte length with the top bit set,
     * but no payload. The length must be read as unsigned 32-bit, so the (impossibly large)
     * length fails the read-limit check with "Can't extend the limit". Before the fix,
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
                    e.getMessage().contains("Can't extend the limit"));
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
}
