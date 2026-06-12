/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.PointerBytesStore;
import net.openhft.chronicle.bytes.ReadBytesMarshallable;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class BinaryWireNegativeLengthTest extends WireTestCommon {

    @Test
    public void readWithLengthRejectsNegativeLengthBeforeMutatingOutput() {
        Bytes<?> bytes = Bytes.wrapForRead(new byte[5]);
        bytes.readPosition(5);

        BinaryWire source = new BinaryWire(bytes);
        Bytes<?> outputBytes = Bytes.allocateElasticOnHeap();
        Wire outputWire = WireType.TEXT.apply(outputBytes);
        long outputWritePosition = outputBytes.writePosition();

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> source.readWithLength(outputWire, -1));

        assertTrue(e.getMessage().contains("Invalid length"));
        assertEquals(outputWritePosition, outputBytes.writePosition());
        assertTrue(bytes.readLimit() >= bytes.readPosition());
    }

    @Test
    public void copyOneRejectsBytesLength32MaxUnsignedLength() {
        Bytes<?> bytes = Bytes.wrapForRead(negativeBytesLength32Payload());
        BinaryWire source = new BinaryWire(bytes);
        Bytes<?> outputBytes = Bytes.allocateElasticOnHeap();
        Wire outputWire = WireType.TEXT.apply(outputBytes);
        long outputWritePosition = outputBytes.writePosition();

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> source.copyOne(outputWire));

        assertTrue(e.getMessage().contains("bytes remaining between readPosition"));
        assertEquals(5, bytes.readPosition());
        assertTrue(bytes.readLimit() >= bytes.readPosition());
        assertEquals(outputWritePosition, outputBytes.writePosition());
    }

    @Test
    public void copyOneRejectsMaxUnsignedLengthWithoutConsumingFollowingBytes() {
        byte[] payload = {
                (byte) BinaryWireCode.BYTES_LENGTH32,
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                0x01, 0x02, 0x03, 0x04
        };
        Bytes<?> bytes = Bytes.wrapForRead(payload);
        BinaryWire source = new BinaryWire(bytes);
        Bytes<?> outputBytes = Bytes.allocateElasticOnHeap();
        Wire outputWire = WireType.TEXT.apply(outputBytes);
        long outputWritePosition = outputBytes.writePosition();

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> source.copyOne(outputWire));

        assertTrue(e.getMessage().contains("bytes remaining between readPosition"));
        assertEquals(5, bytes.readPosition());
        assertEquals(0x01, bytes.readUnsignedByte(5));
        assertEquals(0x02, bytes.readUnsignedByte(6));
        assertEquals(0x03, bytes.readUnsignedByte(7));
        assertEquals(0x04, bytes.readUnsignedByte(8));
        assertEquals(outputWritePosition, outputBytes.writePosition());
    }

    @Test
    public void objectRejectsBytesLength32MaxUnsignedLength() {
        byte[] payload = {
                (byte) BinaryWireCode.BYTES_LENGTH32,
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                (byte) BinaryWireCode.TRUE
        };
        Bytes<?> bytes = Bytes.wrapForRead(payload);
        BinaryWire wire = new BinaryWire(bytes);

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> wire.getValueIn().object());

        assertTrue(e.getMessage().contains("bytes remaining between readPosition"));
        assertEquals(5, bytes.readPosition());
        assertTrue(bytes.readLimit() >= bytes.readPosition());
        assertEquals(BinaryWireCode.TRUE, bytes.readUnsignedByte(5));
    }

    @Test
    public void objectRejectsBytesLength32LengthBeyondCurrentReadLimit() {
        byte[] payload = {
                (byte) BinaryWireCode.BYTES_LENGTH32,
                0x04, 0x00, 0x00, 0x00,
                (byte) BinaryWireCode.TRUE,
                0x01, 0x02, 0x03
        };
        Bytes<?> bytes = Bytes.wrapForRead(payload);
        assertEquals(4, bytes.readInt(1));
        bytes.readLimit(6);
        BinaryWire wire = new BinaryWire(bytes);

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> wire.getValueIn().object());

        assertTrue(e.getMessage().contains("bytes remaining between readPosition"));
        assertEquals(5, bytes.readPosition());
        assertEquals(6, bytes.readLimit());
        assertTrue(bytes.readRemaining() >= 0);
        assertEquals(BinaryWireCode.TRUE, bytes.readUnsignedByte(5));
        assertEquals(0x01, bytes.readUnsignedByte(6));
        assertEquals(0x02, bytes.readUnsignedByte(7));
        assertEquals(0x03, bytes.readUnsignedByte(8));
    }

    @Test
    public void objectRejectsUncheckedBytesLength32U8ArrayBeyondCurrentReadLimit() {
        byte[] payload = {
                (byte) BinaryWireCode.BYTES_LENGTH32,
                0x04, 0x00, 0x00, 0x00,
                (byte) BinaryWireCode.U8_ARRAY,
                0x01, 0x02, 0x03
        };
        Bytes<?> bytes = Bytes.wrapForRead(payload).unchecked(true);
        assertEquals(4, bytes.readInt(1));
        bytes.readLimit(6);
        BinaryWire wire = new BinaryWire(bytes);

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> wire.getValueIn().object());

        assertTrue(e.getMessage().contains("bytes remaining between readPosition"));
        assertEquals(5, bytes.readPosition());
        assertEquals(6, bytes.readLimit());
        assertTrue(bytes.readRemaining() >= 0);
        assertEquals(BinaryWireCode.U8_ARRAY, bytes.readUnsignedByte(5));
        assertEquals(0x01, bytes.readUnsignedByte(6));
        assertEquals(0x02, bytes.readUnsignedByte(7));
        assertEquals(0x03, bytes.readUnsignedByte(8));
    }

    @Test
    public void bytesStoreRejectsUncheckedBytesLength32U8ArrayBeyondCurrentReadLimit() {
        Bytes<?> bytes = uncheckedLength32U8ArrayWithNarrowReadLimit();
        BinaryWire wire = new BinaryWire(bytes);

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> wire.getValueIn().bytesStore());

        assertTrue(e.getMessage().contains("bytes remaining between readPosition"));
        assertEquals(5, bytes.readPosition());
        assertReadableStateAndPayload(bytes, 5);
    }

    @Test
    public void bytesStoreRejectsZeroLengthBodyBeforeReadingNestedCode() {
        Bytes<?> bytes = uncheckedLength32WithZeroDeclaredBody();
        BinaryWire wire = new BinaryWire(bytes);

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> wire.getValueIn().bytesStore());

        assertTrue(e.getMessage().contains("does not include a value code"));
        assertZeroLengthBodyState(bytes);
    }

    @Test
    public void bytesLiteralToBytesRejectsUncheckedBytesLength32U8ArrayBeyondCurrentReadLimit() {
        Bytes<?> bytes = uncheckedLength32U8ArrayWithNarrowReadLimit();
        BinaryWire wire = new BinaryWire(bytes);
        Bytes<?> output = Bytes.allocateElasticOnHeap();
        long outputWritePosition = output.writePosition();

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> wire.getValueIn().bytesLiteral(output));

        assertTrue(e.getMessage().contains("bytes remaining between readPosition"));
        assertEquals(outputWritePosition, output.writePosition());
        assertEquals(5, bytes.readPosition());
        assertReadableStateAndPayload(bytes, 5);
    }

    @Test
    public void bytesLiteralRejectsUncheckedBytesLength32U8ArrayBeyondCurrentReadLimit() {
        Bytes<?> bytes = uncheckedLength32U8ArrayWithNarrowReadLimit();
        BinaryWire wire = new BinaryWire(bytes);

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> wire.getValueIn().bytesLiteral());

        assertTrue(e.getMessage().contains("bytes remaining between readPosition"));
        assertEquals(5, bytes.readPosition());
        assertReadableStateAndPayload(bytes, 5);
    }

    @Test
    public void bytesSetRejectsUncheckedBytesLength32U8ArrayBeyondCurrentReadLimit() {
        Bytes<?> bytes = uncheckedLength32U8ArrayWithNarrowReadLimit();
        BinaryWire wire = new BinaryWire(bytes);
        PointerBytesStore pointer = BytesStore.nativePointer();

        try {
            IORuntimeException e = assertThrows(IORuntimeException.class,
                    () -> wire.getValueIn().bytesSet(pointer));

            assertTrue(e.getMessage().contains("bytes remaining between readPosition"));
            assertEquals(5, bytes.readPosition());
            assertReadableStateAndPayload(bytes, 5);
        } finally {
            pointer.releaseLast();
        }
    }

    @Test
    public void bytesSetRejectsZeroLengthBodyBeforeReadingNestedCode() {
        Bytes<?> bytes = uncheckedLength32WithZeroDeclaredBody();
        BinaryWire wire = new BinaryWire(bytes);
        PointerBytesStore pointer = BytesStore.nativePointer();

        try {
            IORuntimeException e = assertThrows(IORuntimeException.class,
                    () -> wire.getValueIn().bytesSet(pointer));

            assertTrue(e.getMessage().contains("does not include a value code"));
            assertZeroLengthBodyState(bytes);
        } finally {
            pointer.releaseLast();
        }
    }

    @Test
    public void bytesMatchRejectsUncheckedBytesLength32U8ArrayBeyondCurrentReadLimit() {
        Bytes<?> bytes = uncheckedLength32U8ArrayWithNarrowReadLimit();
        BinaryWire wire = new BinaryWire(bytes);
        Bytes<?> compare = Bytes.wrapForRead(new byte[]{0x01, 0x02, 0x03});

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> wire.getValueIn().bytesMatch(compare, matched -> fail("Should not compare bytes")));

        assertTrue(e.getMessage().contains("bytes remaining between readPosition"));
        assertEquals(5, bytes.readPosition());
        assertReadableStateAndPayload(bytes, 5);
    }

    @Test
    public void bytesMatchRejectsZeroLengthBodyBeforeInvokingConsumer() {
        Bytes<?> bytes = uncheckedLength32WithZeroDeclaredBody();
        BinaryWire wire = new BinaryWire(bytes);
        Bytes<?> compare = Bytes.wrapForRead(new byte[0]);
        AtomicBoolean invoked = new AtomicBoolean();

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> wire.getValueIn().bytesMatch(compare, matched -> invoked.set(true)));

        assertTrue(e.getMessage().contains("does not include a value code"));
        assertFalse(invoked.get());
        assertZeroLengthBodyState(bytes);
    }

    @Test
    public void bytesToBytesOutRejectsBeforeClearingOutput() {
        Bytes<?> bytes = uncheckedLength32U8ArrayWithNarrowReadLimit();
        BinaryWire wire = new BinaryWire(bytes);
        Bytes<?> output = Bytes.allocateElasticOnHeap();
        output.writeByte((byte) 0x55);
        long outputWritePosition = output.writePosition();

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> wire.getValueIn().bytes(output));

        assertTrue(e.getMessage().contains("bytes remaining between readPosition"));
        assertEquals(outputWritePosition, output.writePosition());
        assertEquals(0x55, output.readUnsignedByte(0));
        assertEquals(5, bytes.readPosition());
        assertReadableStateAndPayload(bytes, 5);
    }

    @Test
    public void bytesToBytesOutRejectsZeroLengthBodyBeforeClearingOutput() {
        Bytes<?> bytes = uncheckedLength32WithZeroDeclaredBody();
        BinaryWire wire = new BinaryWire(bytes);
        Bytes<?> output = Bytes.allocateElasticOnHeap();
        output.writeByte((byte) 0x66);
        long outputWritePosition = output.writePosition();

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> wire.getValueIn().bytes(output));

        assertTrue(e.getMessage().contains("does not include a value code"));
        assertEquals(outputWritePosition, output.writePosition());
        assertEquals(0x66, output.readUnsignedByte(0));
        assertZeroLengthBodyState(bytes);
    }

    @Test
    public void bytesToBytesOutFallbackRejectsWithoutConsumingBeyondDeclaredBody() {
        Bytes<?> bytes = bytesLength8OneByteBodyWithTrailingPaddingAndTrue();
        long originalLimit = bytes.readLimit();
        BinaryWire wire = new BinaryWire(bytes);
        Bytes<?> output = Bytes.allocateElasticOnHeap();
        output.writeByte((byte) 0x66);
        long outputWritePosition = output.writePosition();

        RuntimeException e = assertThrows(RuntimeException.class,
                () -> wire.getValueIn().bytes(output));

        assertNotNull(e);
        assertEquals(outputWritePosition, output.writePosition());
        assertEquals(0x66, output.readUnsignedByte(0));
        assertTrue(bytes.readPosition() <= 3);
        assertEquals(originalLimit, bytes.readLimit());
        assertTrailingPaddingAndTrue(bytes);
    }

    @Test
    public void bytesToBytesOutTypePrefixRejectsWithoutConsumingBeyondDeclaredBody() {
        Bytes<?> bytes = bytesLength8OneByteBodyTypePrefixWithTrailingBytes();
        long originalLimit = bytes.readLimit();
        BinaryWire wire = new BinaryWire(bytes);
        Bytes<?> output = Bytes.allocateElasticOnHeap();
        output.writeByte((byte) 0x66);
        long outputWritePosition = output.writePosition();

        RuntimeException e = assertThrows(RuntimeException.class,
                () -> wire.getValueIn().bytes(output));

        assertNotNull(e);
        assertEquals(outputWritePosition, output.writePosition());
        assertEquals(0x66, output.readUnsignedByte(0));
        assertTrue(bytes.readPosition() <= 3);
        assertEquals(originalLimit, bytes.readLimit());
        assertTrailingTypePrefixBytes(bytes);
    }

    @Test
    public void bytesToBytesOutReadsEmptyU8ArrayLengthOne() {
        byte[] payload = {
                (byte) BinaryWireCode.BYTES_LENGTH8,
                0x01,
                (byte) BinaryWireCode.U8_ARRAY,
                0x55
        };
        Bytes<?> bytes = Bytes.wrapForRead(payload);
        BinaryWire wire = new BinaryWire(bytes);
        Bytes<?> output = Bytes.allocateElasticOnHeap();
        output.writeByte((byte) 0x66);

        wire.getValueIn().bytes(output);

        assertEquals(0, output.writePosition());
        assertEquals(3, bytes.readPosition());
        assertEquals(payload.length, bytes.readLimit());
        assertEquals(0x55, bytes.readUnsignedByte(3));
    }

    @Test
    public void bytesToBytesOutReadsNullLengthOne() {
        byte[] payload = {
                (byte) BinaryWireCode.BYTES_LENGTH8,
                0x01,
                (byte) BinaryWireCode.NULL,
                0x55
        };
        Bytes<?> bytes = Bytes.wrapForRead(payload);
        BinaryWire wire = new BinaryWire(bytes);
        Bytes<?> output = Bytes.allocateElasticOnHeap();
        output.writeByte((byte) 0x66);

        wire.getValueIn().bytes(output);

        assertEquals(0, output.writePosition());
        assertEquals(3, bytes.readPosition());
        assertEquals(payload.length, bytes.readLimit());
        assertEquals(0x55, bytes.readUnsignedByte(3));
    }

    @Test
    public void bytesStoreToBytesRejectsBeforeClearingTarget() {
        Bytes<?> bytes = uncheckedLength32U8ArrayWithNarrowReadLimit();
        BinaryWire wire = new BinaryWire(bytes);
        Bytes<?> target = Bytes.allocateElasticOnHeap();
        target.writeByte((byte) 0x55);
        long targetWritePosition = target.writePosition();

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> ((BinaryWire.BinaryValueIn) wire.getValueIn()).bytesStore(target));

        assertTrue(e.getMessage().contains("bytes remaining between readPosition"));
        assertEquals(targetWritePosition, target.writePosition());
        assertEquals(0x55, target.readUnsignedByte(0));
        assertEquals(5, bytes.readPosition());
        assertReadableStateAndPayload(bytes, 5);
    }

    @Test
    public void bytesStoreToBytesRejectsZeroLengthBodyBeforeClearingTarget() {
        Bytes<?> bytes = uncheckedLength32WithZeroDeclaredBody();
        BinaryWire wire = new BinaryWire(bytes);
        Bytes<?> target = Bytes.allocateElasticOnHeap();
        target.writeByte((byte) 0x66);
        long targetWritePosition = target.writePosition();

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> ((BinaryWire.BinaryValueIn) wire.getValueIn()).bytesStore(target));

        assertTrue(e.getMessage().contains("does not include a value code"));
        assertEquals(targetWritePosition, target.writePosition());
        assertEquals(0x66, target.readUnsignedByte(0));
        assertZeroLengthBodyState(bytes);
    }

    @Test
    public void bytesMarshallableRejectsBeforeInvokingCallback() {
        Bytes<?> bytes = uncheckedLength32U8ArrayWithZeroPayloadLength();
        BinaryWire wire = new BinaryWire(bytes);
        AtomicBoolean invoked = new AtomicBoolean();

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> wire.getValueIn().bytes((ReadBytesMarshallable) in -> invoked.set(true)));

        assertTrue(e.getMessage().contains("does not include a value code"));
        assertFalse(invoked.get());
        assertEquals(5, bytes.readPosition());
        assertEquals(6, bytes.readLimit());
        assertTrue(bytes.readRemaining() >= 0);
    }

    @Test
    public void bytesMarshallableRejectsZeroLengthBodyBeforeInvokingCallback() {
        Bytes<?> bytes = uncheckedLength32WithZeroDeclaredBody();
        BinaryWire wire = new BinaryWire(bytes);
        AtomicBoolean invoked = new AtomicBoolean();

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> wire.getValueIn().bytes((ReadBytesMarshallable) in -> invoked.set(true)));

        assertTrue(e.getMessage().contains("does not include a value code"));
        assertFalse(invoked.get());
        assertZeroLengthBodyState(bytes);
    }

    @Test
    public void bytesArrayRejectsBeforeAllocationOrRead() {
        Bytes<?> bytes = uncheckedLength32U8ArrayWithNarrowReadLimit();
        BinaryWire wire = new BinaryWire(bytes);

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> wire.getValueIn().bytes((byte[]) null));

        assertTrue(e.getMessage().contains("bytes remaining between readPosition"));
        assertEquals(5, bytes.readPosition());
        assertReadableStateAndPayload(bytes, 5);
    }

    @Test
    public void bytesArrayRejectsZeroLengthBodyBeforeAllocationOrRead() {
        Bytes<?> bytes = uncheckedLength32WithZeroDeclaredBody();
        BinaryWire wire = new BinaryWire(bytes);
        byte[] using = {(byte) 0x66};

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> wire.getValueIn().bytes(using));

        assertTrue(e.getMessage().contains("does not include a value code"));
        assertEquals((byte) 0x66, using[0]);
        assertZeroLengthBodyState(bytes);
    }

    @Test
    public void skipValueRejectsBeforeSkippingPastReadLimit() {
        Bytes<?> bytes = uncheckedLength32U8ArrayWithNarrowReadLimit();
        BinaryWire wire = new BinaryWire(bytes);

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> wire.getValueIn().skipValue());

        assertTrue(e.getMessage().contains("bytes remaining between readPosition"));
        assertEquals(5, bytes.readPosition());
        assertReadableStateAndPayload(bytes, 5);
    }

    @Test
    public void sequenceRejectsLengthBeyondCurrentReadLimitBeforeInvokingReader() {
        Bytes<?> bytes = length32BodyWithNarrowReadLimit((byte) BinaryWireCode.TRUE);
        BinaryWire wire = new BinaryWire(bytes);
        AtomicBoolean invoked = new AtomicBoolean();

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> wire.getValueIn().sequence("holder", (holder, valueIn) -> invoked.set(true)));

        assertTrue(e.getMessage().contains("bytes remaining between readPosition"));
        assertFalse(invoked.get());
        assertEquals(5, bytes.readPosition());
        assertLength32BodyState(bytes, 5, BinaryWireCode.TRUE);
    }

    @Test
    public void marshallableRejectsLengthBeyondCurrentReadLimitBeforeInvokingReader() {
        Bytes<?> bytes = length32BodyWithNarrowReadLimit((byte) BinaryWireCode.TRUE);
        BinaryWire wire = new BinaryWire(bytes);
        AtomicBoolean invoked = new AtomicBoolean();

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> wire.getValueIn().marshallable((ReadMarshallable) in -> invoked.set(true)));

        assertTrue(e.getMessage().contains("bytes remaining between readPosition"));
        assertFalse(invoked.get());
        assertEquals(5, bytes.readPosition());
        assertLength32BodyState(bytes, 5, BinaryWireCode.TRUE);
    }

    @Test
    public void consumePaddingRejectsPadding32BeyondCurrentReadLimit() {
        Bytes<?> bytes = padding32WithNarrowReadLimit();
        BinaryWire wire = new BinaryWire(bytes);

        IORuntimeException e = assertThrows(IORuntimeException.class, wire::consumePadding);

        assertTrue(e.getMessage().contains("bytes remaining between readPosition"));
        assertEquals(5, bytes.readPosition());
        assertEquals(5, bytes.readLimit());
        assertEquals(0, bytes.readRemaining());
        assertTrue(bytes.readPosition() <= bytes.readLimit());
    }

    @Test
    public void consumePaddingRejectsPadding32TruncatedLengthPrefix() {
        Bytes<?> bytes = padding32WithTruncatedLengthPrefix();
        BinaryWire wire = new BinaryWire(bytes);

        IORuntimeException e = assertThrows(IORuntimeException.class, wire::consumePadding);

        assertTrue(e.getMessage().contains("bytes remaining between readPosition"));
        assertTruncatedPadding32PrefixState(bytes);
    }

    @Test
    public void copyOneRejectsPadding32BeyondCurrentReadLimit() {
        Bytes<?> bytes = padding32WithNarrowReadLimit();
        BinaryWire wire = new BinaryWire(bytes);
        Wire outputWire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> wire.copyOne(outputWire));

        assertTrue(e.getMessage().contains("bytes remaining between readPosition"));
        assertEquals(5, bytes.readPosition());
        assertEquals(5, bytes.readLimit());
        assertEquals(0, bytes.readRemaining());
        assertTrue(bytes.readPosition() <= bytes.readLimit());
    }

    @Test
    public void copyOneRejectsPadding32TruncatedLengthPrefix() {
        Bytes<?> bytes = padding32WithTruncatedLengthPrefix();
        BinaryWire wire = new BinaryWire(bytes);
        Bytes<?> outputBytes = Bytes.allocateElasticOnHeap();
        outputBytes.writeByte((byte) 0x66);
        Wire outputWire = WireType.TEXT.apply(outputBytes);
        long outputWritePosition = outputBytes.writePosition();

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> wire.copyOne(outputWire));

        assertTrue(e.getMessage().contains("bytes remaining between readPosition"));
        assertEquals(outputWritePosition, outputBytes.writePosition());
        assertEquals(0x66, outputBytes.readUnsignedByte(0));
        assertTruncatedPadding32PrefixState(bytes);
    }

    @Test
    public void consumePaddingSkipsPadding32WithinCurrentReadLimit() {
        byte[] payload = {
                (byte) BinaryWireCode.PADDING32,
                0x04, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00,
                (byte) BinaryWireCode.TRUE
        };
        Bytes<?> bytes = Bytes.wrapForRead(payload);
        BinaryWire wire = new BinaryWire(bytes);

        wire.consumePadding();

        assertEquals(9, bytes.readPosition());
        assertEquals(payload.length, bytes.readLimit());
        assertEquals(BinaryWireCode.TRUE, bytes.readUnsignedByte(9));
    }

    @Test
    public void consumeNextRejectsBytesLength32BeyondCurrentReadLimit() {
        Bytes<?> bytes = uncheckedLength32U8ArrayWithNarrowReadLimit();
        BinaryWire wire = new BinaryWire(bytes);

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> ((BinaryWire.BinaryValueIn) wire.getValueIn()).consumeNext());

        assertTrue(e.getMessage().contains("bytes remaining between readPosition"));
        assertEquals(5, bytes.readPosition());
        assertReadableStateAndPayload(bytes, 5);
    }

    @Test
    public void copyOneRejectsBytesLength32TruncatedLengthPrefix() {
        Bytes<?> bytes = bytesLength32WithTruncatedLengthPrefix();
        BinaryWire wire = new BinaryWire(bytes);
        Bytes<?> outputBytes = Bytes.allocateElasticOnHeap();
        outputBytes.writeByte((byte) 0x66);
        Wire outputWire = WireType.TEXT.apply(outputBytes);
        long outputWritePosition = outputBytes.writePosition();

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> wire.copyOne(outputWire));

        assertTrue(e.getMessage().contains("Length prefix"));
        assertEquals(outputWritePosition, outputBytes.writePosition());
        assertEquals(0x66, outputBytes.readUnsignedByte(0));
        assertTruncatedLength32PrefixState(bytes);
    }

    @Test
    public void skipValueRejectsBytesLength32TruncatedLengthPrefix() {
        Bytes<?> bytes = bytesLength32WithTruncatedLengthPrefix();
        BinaryWire wire = new BinaryWire(bytes);

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> wire.getValueIn().skipValue());

        assertTrue(e.getMessage().contains("Length prefix"));
        assertTruncatedLength32PrefixState(bytes);
    }

    @Test
    public void bytesStoreRejectsBytesLength32TruncatedLengthPrefix() {
        Bytes<?> bytes = bytesLength32WithTruncatedLengthPrefix();
        BinaryWire wire = new BinaryWire(bytes);

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> wire.getValueIn().bytesStore());

        assertTrue(e.getMessage().contains("Length prefix"));
        assertTruncatedLength32PrefixState(bytes);
    }

    @Test
    public void objectRejectsBytesLength32TruncatedLengthPrefix() {
        Bytes<?> bytes = bytesLength32WithTruncatedLengthPrefix();
        BinaryWire wire = new BinaryWire(bytes);

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> wire.getValueIn().object());

        assertTrue(e.getMessage().contains("Length prefix"));
        assertTruncatedLength32PrefixState(bytes);
    }

    @Test
    public void consumeNextRejectsBytesLength32TruncatedLengthPrefix() {
        Bytes<?> bytes = bytesLength32WithTruncatedLengthPrefix();
        BinaryWire wire = new BinaryWire(bytes);

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> ((BinaryWire.BinaryValueIn) wire.getValueIn()).consumeNext());

        assertTrue(e.getMessage().contains("Length prefix"));
        assertTruncatedLength32PrefixState(bytes);
    }

    @Test
    public void skipValueRejectsBytesLength16TruncatedLengthPrefix() {
        Bytes<?> bytes = bytesLength16WithTruncatedLengthPrefix();
        BinaryWire wire = new BinaryWire(bytes);

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> wire.getValueIn().skipValue());

        assertTrue(e.getMessage().contains("Length prefix"));
        assertTruncatedLength16PrefixState(bytes);
    }

    @Test
    public void skipValueRejectsBytesLength8TruncatedLengthPrefix() {
        Bytes<?> bytes = bytesLength8WithTruncatedLengthPrefix();
        BinaryWire wire = new BinaryWire(bytes);

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> wire.getValueIn().skipValue());

        assertTrue(e.getMessage().contains("Length prefix"));
        assertTruncatedLength8PrefixState(bytes);
    }

    @Test
    public void skipValueRejectsTypePrefixTruncatedStopBitPrefix() {
        Bytes<?> bytes = typePrefixWithTruncatedStopBitPrefix();
        BinaryWire wire = new BinaryWire(bytes);

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> wire.getValueIn().skipValue());

        assertTrue(e.getMessage().contains("Stop bit length prefix"));
        assertTruncatedTypePrefixStopBitState(bytes);
    }

    @Test
    public void textRejectsStringAnyTruncatedStopBitPrefix() {
        Bytes<?> bytes = stringAnyWithTruncatedStopBitPrefix();
        BinaryWire wire = new BinaryWire(bytes);

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> wire.getValueIn().text());

        assertTrue(e.getMessage().contains("Stop bit length prefix"));
        assertStringAnyState(bytes, 1, 1);
    }

    @Test
    public void textRejectsStringAnyBodyBeyondCurrentReadLimit() {
        Bytes<?> bytes = stringAnyWithBodyBeyondCurrentReadLimit();
        BinaryWire wire = new BinaryWire(bytes);

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> wire.getValueIn().text());

        assertTrue(e.getMessage().contains("Text length"));
        assertStringAnyState(bytes, 2, 2);
    }

    @Test
    public void objectRejectsStringAnyBodyBeyondCurrentReadLimit() {
        Bytes<?> bytes = stringAnyWithBodyBeyondCurrentReadLimit();
        BinaryWire wire = new BinaryWire(bytes);

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> wire.getValueIn().object());

        assertTrue(e.getMessage().contains("Text length"));
        assertStringAnyState(bytes, 2, 2);
    }

    @Test
    public void skipValueRejectsStringAnyTruncatedStopBitPrefix() {
        Bytes<?> bytes = stringAnyWithTruncatedStopBitPrefix();
        BinaryWire wire = new BinaryWire(bytes);

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> wire.getValueIn().skipValue());

        assertTrue(e.getMessage().contains("Stop bit length prefix"));
        assertEquals(0, bytes.readPosition());
        assertEquals(1, bytes.readLimit());
        assertTrue(bytes.readPosition() <= bytes.readLimit());
        assertStringAnyBytes(bytes);
    }

    @Test
    public void consumeNextRejectsStringAnyBodyBeyondCurrentReadLimit() {
        Bytes<?> bytes = stringAnyWithBodyBeyondCurrentReadLimit();
        BinaryWire wire = new BinaryWire(bytes);

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> ((BinaryWire.BinaryValueIn) wire.getValueIn()).consumeNext());

        assertTrue(e.getMessage().contains("Text length"));
        assertStringAnyState(bytes, 2, 2);
    }

    @Test
    public void readTextRejectsStringAnyBodyBeyondCurrentReadLimit() {
        Bytes<?> bytes = stringAnyBodyWithBodyBeyondCurrentReadLimit();
        BinaryWire wire = new BinaryWire(bytes);
        StringBuilder sb = new StringBuilder("sentinel");

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> wire.readText(BinaryWireCode.STRING_ANY, sb));

        assertTrue(e.getMessage().contains("Text length"));
        assertEquals("sentinel", sb.toString());
        assertStringAnyBodyState(bytes, 1, 1);
    }

    @Test
    public void textToStringBuilderRejectsStringAnyBodyBeyondCurrentReadLimitWithoutMutation() {
        Bytes<?> bytes = stringAnyWithBodyBeyondCurrentReadLimit();
        BinaryWire wire = new BinaryWire(bytes);
        StringBuilder sb = new StringBuilder("sentinel");

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> wire.getValueIn().textTo(sb));

        assertTrue(e.getMessage().contains("Text length"));
        assertEquals("sentinel", sb.toString());
        assertStringAnyState(bytes, 2, 2);
    }

    @Test
    public void textToBytesRejectsStringAnyBodyBeyondCurrentReadLimitWithoutMutation() {
        Bytes<?> bytes = stringAnyWithBodyBeyondCurrentReadLimit();
        BinaryWire wire = new BinaryWire(bytes);
        Bytes<?> target = Bytes.allocateElasticOnHeap();
        target.writeByte((byte) 0x66);
        long writePosition = target.writePosition();

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> wire.getValueIn().textTo(target));

        assertTrue(e.getMessage().contains("Text length"));
        assertEquals(writePosition, target.writePosition());
        assertEquals(0x66, target.readUnsignedByte(0));
        assertStringAnyState(bytes, 2, 2);
    }

    @Test
    public void copyOneRejectsStringAnyTruncatedStopBitPrefixWithoutOutputMutation() {
        Bytes<?> bytes = stringAnyWithTruncatedStopBitPrefix();
        BinaryWire wire = new BinaryWire(bytes);
        Bytes<?> outputBytes = Bytes.allocateElasticOnHeap();
        outputBytes.writeByte((byte) 0x66);
        Wire outputWire = WireType.TEXT.apply(outputBytes);
        long outputWritePosition = outputBytes.writePosition();

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> wire.copyOne(outputWire));

        assertTrue(e.getMessage().contains("Stop bit length prefix"));
        assertEquals(outputWritePosition, outputBytes.writePosition());
        assertEquals(0x66, outputBytes.readUnsignedByte(0));
        assertStringAnyState(bytes, 1, 1);
    }

    @Test
    public void copyOneRejectsStringAnyBodyBeyondCurrentReadLimitWithoutOutputMutation() {
        Bytes<?> bytes = stringAnyWithBodyBeyondCurrentReadLimit();
        BinaryWire wire = new BinaryWire(bytes);
        Bytes<?> outputBytes = Bytes.allocateElasticOnHeap();
        outputBytes.writeByte((byte) 0x66);
        Wire outputWire = WireType.TEXT.apply(outputBytes);
        long outputWritePosition = outputBytes.writePosition();

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> wire.copyOne(outputWire));

        assertTrue(e.getMessage().contains("Text length"));
        assertEquals(outputWritePosition, outputBytes.writePosition());
        assertEquals(0x66, outputBytes.readUnsignedByte(0));
        assertStringAnyState(bytes, 2, 2);
    }

    @Test
    public void copyOneStillCopiesValidStringAny() {
        Bytes<?> bytes = validStringAny();
        Bytes<?> outputBytes = Bytes.allocateElasticOnHeap();
        Wire outputWire = WireType.TEXT.apply(outputBytes);

        new BinaryWire(bytes).copyOne(outputWire);

        assertTrue(outputBytes.toString().contains("test"));
    }

    @Test
    public void textToStringBuilderStillReadsValidStringAny() {
        Bytes<?> bytes = validStringAny();
        BinaryWire wire = new BinaryWire(bytes);
        StringBuilder sb = new StringBuilder("sentinel");

        wire.getValueIn().textTo(sb);

        assertEquals("test", sb.toString());
        assertEquals(6, bytes.readPosition());
        assertEquals(BinaryWireCode.TRUE, bytes.readUnsignedByte(6));
    }

    @Test
    public void stringAnyValidTextStillReads() {
        Bytes<?> bytes = validStringAny();
        BinaryWire wire = new BinaryWire(bytes);

        assertEquals("test", wire.getValueIn().text());
        assertEquals(6, bytes.readPosition());
        assertEquals(BinaryWireCode.TRUE, bytes.readUnsignedByte(6));
    }

    @Test
    public void stringAnyValidNullStillReads() {
        Bytes<?> bytes = stringAnyNull();
        BinaryWire wire = new BinaryWire(bytes);

        assertNull(wire.getValueIn().text());
        assertEquals(bytes.readLimit(), bytes.readPosition());
    }

    @Test
    public void textRejectsCompactStringBodyBeyondCurrentReadLimit() {
        Bytes<?> bytes = compactStringWithBodyBeyondCurrentReadLimit();
        BinaryWire wire = new BinaryWire(bytes);

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> wire.getValueIn().text());

        assertTrue(e.getMessage().contains("Text length"));
        assertCompactStringState(bytes, 1, 1);
    }

    @Test
    public void textToStringBuilderRejectsCompactStringBodyBeyondCurrentReadLimitWithoutMutation() {
        Bytes<?> bytes = compactStringWithBodyBeyondCurrentReadLimit();
        BinaryWire wire = new BinaryWire(bytes);
        StringBuilder sb = new StringBuilder("sentinel");

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> wire.getValueIn().textTo(sb));

        assertTrue(e.getMessage().contains("Text length"));
        assertEquals("sentinel", sb.toString());
        assertCompactStringState(bytes, 1, 1);
    }

    @Test
    public void textToBytesRejectsCompactStringBodyBeyondCurrentReadLimitWithoutMutation() {
        Bytes<?> bytes = compactStringWithBodyBeyondCurrentReadLimit();
        BinaryWire wire = new BinaryWire(bytes);
        Bytes<?> target = Bytes.allocateElasticOnHeap();
        target.writeByte((byte) 0x66);
        long writePosition = target.writePosition();

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> wire.getValueIn().textTo(target));

        assertTrue(e.getMessage().contains("Text length"));
        assertEquals(writePosition, target.writePosition());
        assertEquals(0x66, target.readUnsignedByte(0));
        assertCompactStringState(bytes, 1, 1);
    }

    @Test
    public void compactStringValidTextStillReads() {
        Bytes<?> bytes = validCompactString();
        BinaryWire wire = new BinaryWire(bytes);

        assertEquals("test", wire.getValueIn().text());
        assertEquals(5, bytes.readPosition());
        assertEquals(BinaryWireCode.TRUE, bytes.readUnsignedByte(5));
    }

    @Test
    public void readLengthRejectsTypePrefixBeyondCurrentReadLimit() {
        Bytes<?> bytes = typePrefixWithNarrowReadLimit();
        BinaryWire wire = new BinaryWire(bytes);

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> wire.getValueIn().skipValue());

        assertTrue(e.getMessage().contains("bytes remaining between readPosition"));
        assertEquals(2, bytes.readPosition());
        assertEquals(2, bytes.readLimit());
        assertEquals(0, bytes.readRemaining());
        assertTrue(bytes.readPosition() <= bytes.readLimit());
    }

    @Test
    public void readTextRejectsPadding32BeyondCurrentReadLimit() {
        Bytes<?> bytes = padding32BodyWithNarrowReadLimit();
        BinaryWire wire = new BinaryWire(bytes);
        StringBuilder sb = new StringBuilder();

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> wire.readText(BinaryWireCode.PADDING32, sb));

        assertTrue(e.getMessage().contains("bytes remaining between readPosition"));
        assertEquals(4, bytes.readPosition());
        assertEquals(4, bytes.readLimit());
        assertEquals(0, bytes.readRemaining());
        assertTrue(bytes.readPosition() <= bytes.readLimit());
    }

    @Test
    public void readTextRejectsPadding32TruncatedLengthPrefix() {
        Bytes<?> bytes = padding32BodyWithTruncatedLengthPrefix();
        BinaryWire wire = new BinaryWire(bytes);
        StringBuilder sb = new StringBuilder("sentinel");

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> wire.readText(BinaryWireCode.PADDING32, sb));

        assertTrue(e.getMessage().contains("bytes remaining between readPosition"));
        assertEquals("sentinel", sb.toString());
        assertTruncatedPadding32BodyPrefixState(bytes);
    }

    @Test
    public void bytesStoreStringBuilderRejectsBeforeClearingTarget() {
        Bytes<?> bytes = uncheckedLength32U8ArrayWithNarrowReadLimit();
        BinaryWire wire = new BinaryWire(bytes);
        StringBuilder sb = new StringBuilder("sentinel");

        RuntimeException e = assertThrows(RuntimeException.class,
                () -> ((BinaryWire.BinaryValueIn) wire.getValueIn()).bytesStore(sb));

        assertNotNull(e);
        assertEquals("sentinel", sb.toString());
        assertEquals(5, bytes.readPosition());
        assertReadableStateAndPayload(bytes, 5);
    }

    @Test
    public void bytesStoreStringBuilderRejectsZeroLengthBodyBeforeClearingTarget() {
        Bytes<?> bytes = uncheckedLength32WithZeroDeclaredBody();
        BinaryWire wire = new BinaryWire(bytes);
        StringBuilder sb = new StringBuilder("sentinel");

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> ((BinaryWire.BinaryValueIn) wire.getValueIn()).bytesStore(sb));

        assertTrue(e.getMessage().contains("does not include a value code"));
        assertEquals("sentinel", sb.toString());
        assertZeroLengthBodyState(bytes);
    }

    @Test
    public void bytesStoreTypePrefixRejectsWithoutConsumingBeyondDeclaredBody() {
        Bytes<?> bytes = bytesLength8OneByteBodyTypePrefixWithTrailingBytes();
        long originalLimit = bytes.readLimit();
        BinaryWire wire = new BinaryWire(bytes);

        RuntimeException e = assertThrows(RuntimeException.class,
                () -> wire.getValueIn().bytesStore());

        assertNotNull(e);
        assertTrue(bytes.readPosition() <= 3);
        assertEquals(originalLimit, bytes.readLimit());
        assertTrailingTypePrefixBytes(bytes);
    }

    @Test
    public void bytesStoreStringBuilderReadsValidPayloadOnly() {
        byte[] payload = {
                (byte) BinaryWireCode.BYTES_LENGTH32,
                0x04, 0x00, 0x00, 0x00,
                (byte) BinaryWireCode.U8_ARRAY,
                'a', 'b', 'c'
        };
        Bytes<?> bytes = Bytes.wrapForRead(payload);
        BinaryWire wire = new BinaryWire(bytes);
        StringBuilder sb = new StringBuilder("sentinel");

        ((BinaryWire.BinaryValueIn) wire.getValueIn()).bytesStore(sb);

        assertEquals("abc", sb.toString());
        assertEquals(payload.length, bytes.readPosition());
        assertEquals(payload.length, bytes.readLimit());
    }

    @Test
    public void bytesStoreStringBuilderFallbackStopsAtDeclaredEnd() {
        byte[] payload = {
                (byte) BinaryWireCode.BYTES_LENGTH8,
                0x01,
                (byte) BinaryWireCode.TRUE,
                0x55, 0x66
        };
        Bytes<?> bytes = Bytes.wrapForRead(payload);
        BinaryWire wire = new BinaryWire(bytes);
        StringBuilder sb = new StringBuilder("sentinel");

        ((BinaryWire.BinaryValueIn) wire.getValueIn()).bytesStore(sb);

        assertNotEquals("sentinel", sb.toString());
        assertEquals(3, bytes.readPosition());
        assertEquals(payload.length, bytes.readLimit());
        assertTrue(bytes.readPosition() < bytes.readLimit());
        assertEquals(0x55, bytes.readUnsignedByte(3));
        assertEquals(0x66, bytes.readUnsignedByte(4));
    }

    @Test
    public void bytesStoreStringBuilderFallbackRejectsBeforeClearingTarget() {
        byte[] payload = {
                (byte) BinaryWireCode.BYTES_LENGTH8,
                0x01,
                (byte) BinaryWireCode.TRUE
        };
        Bytes<?> bytes = Bytes.wrapForRead(payload).unchecked(true);
        bytes.readLimit(2);
        BinaryWire wire = new BinaryWire(bytes);
        StringBuilder sb = new StringBuilder("sentinel");

        IORuntimeException e = assertThrows(IORuntimeException.class,
                () -> ((BinaryWire.BinaryValueIn) wire.getValueIn()).bytesStore(sb));

        assertTrue(e.getMessage().contains("bytes remaining between readPosition"));
        assertEquals("sentinel", sb.toString());
        assertEquals(2, bytes.readPosition());
        assertEquals(2, bytes.readLimit());
        assertTrue(bytes.readPosition() <= bytes.readLimit());
    }

    private static Bytes<?> uncheckedLength32U8ArrayWithNarrowReadLimit() {
        byte[] payload = {
                (byte) BinaryWireCode.BYTES_LENGTH32,
                0x04, 0x00, 0x00, 0x00,
                (byte) BinaryWireCode.U8_ARRAY,
                0x01, 0x02, 0x03
        };
        Bytes<?> bytes = Bytes.wrapForRead(payload).unchecked(true);
        assertEquals(4, bytes.readInt(1));
        bytes.readLimit(6);
        return bytes;
    }

    private static Bytes<?> bytesLength8OneByteBodyWithTrailingPaddingAndTrue() {
        byte[] payload = {
                (byte) BinaryWireCode.BYTES_LENGTH8,
                0x01,
                (byte) BinaryWireCode.PADDING32,
                0x04, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00,
                (byte) BinaryWireCode.TRUE
        };
        return Bytes.wrapForRead(payload).unchecked(true);
    }

    private static Bytes<?> bytesLength8OneByteBodyTypePrefixWithTrailingBytes() {
        byte[] payload = {
                (byte) BinaryWireCode.BYTES_LENGTH8,
                0x01,
                (byte) BinaryWireCode.TYPE_PREFIX,
                0x03,
                'x', 'x', 'x',
                (byte) BinaryWireCode.U8_ARRAY,
                0x55
        };
        return Bytes.wrapForRead(payload).unchecked(true);
    }

    private static Bytes<?> padding32WithNarrowReadLimit() {
        byte[] payload = {
                (byte) BinaryWireCode.PADDING32,
                0x04, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00
        };
        Bytes<?> bytes = Bytes.wrapForRead(payload).unchecked(true);
        bytes.readLimit(5);
        return bytes;
    }

    private static Bytes<?> padding32WithTruncatedLengthPrefix() {
        // Backing bytes include the full length prefix; readLimit is narrowed to prove they are not consumed.
        byte[] payload = {
                (byte) BinaryWireCode.PADDING32,
                0x04, 0x00, 0x00, 0x00
        };
        Bytes<?> bytes = Bytes.wrapForRead(payload).unchecked(true);
        bytes.readLimit(1);
        return bytes;
    }

    private static Bytes<?> padding32BodyWithNarrowReadLimit() {
        byte[] payload = {
                0x04, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00
        };
        Bytes<?> bytes = Bytes.wrapForRead(payload).unchecked(true);
        bytes.readLimit(4);
        return bytes;
    }

    private static Bytes<?> padding32BodyWithTruncatedLengthPrefix() {
        // Backing bytes include the full length prefix; readLimit is narrowed to prove they are not consumed.
        byte[] payload = {
                0x04, 0x00, 0x00, 0x00
        };
        Bytes<?> bytes = Bytes.wrapForRead(payload).unchecked(true);
        bytes.readLimit(0);
        return bytes;
    }

    private static Bytes<?> typePrefixWithNarrowReadLimit() {
        byte[] payload = {
                (byte) BinaryWireCode.TYPE_PREFIX,
                0x04,
                't', 'y', 'p', 'e',
                (byte) BinaryWireCode.TRUE
        };
        Bytes<?> bytes = Bytes.wrapForRead(payload).unchecked(true);
        bytes.readLimit(2);
        return bytes;
    }

    private static Bytes<?> typePrefixWithTruncatedStopBitPrefix() {
        // Backing bytes include the stop-bit length and type name; readLimit exposes only the dispatched code byte.
        byte[] payload = {
                (byte) BinaryWireCode.TYPE_PREFIX,
                0x04,
                't', 'y', 'p', 'e',
                (byte) BinaryWireCode.TRUE
        };
        Bytes<?> bytes = Bytes.wrapForRead(payload).unchecked(true);
        bytes.readLimit(1);
        return bytes;
    }

    private static Bytes<?> bytesLength8WithTruncatedLengthPrefix() {
        byte[] payload = {
                (byte) BinaryWireCode.BYTES_LENGTH8,
                0x04,
                (byte) BinaryWireCode.U8_ARRAY,
                0x01, 0x02, 0x03
        };
        Bytes<?> bytes = Bytes.wrapForRead(payload).unchecked(true);
        bytes.readLimit(1);
        return bytes;
    }

    private static Bytes<?> bytesLength16WithTruncatedLengthPrefix() {
        byte[] payload = {
                (byte) BinaryWireCode.BYTES_LENGTH16,
                0x04, 0x00,
                (byte) BinaryWireCode.U8_ARRAY,
                0x01, 0x02, 0x03
        };
        Bytes<?> bytes = Bytes.wrapForRead(payload).unchecked(true);
        bytes.readLimit(1);
        return bytes;
    }

    private static Bytes<?> bytesLength32WithTruncatedLengthPrefix() {
        byte[] payload = {
                (byte) BinaryWireCode.BYTES_LENGTH32,
                0x04, 0x00, 0x00, 0x00,
                (byte) BinaryWireCode.U8_ARRAY,
                0x01, 0x02, 0x03
        };
        Bytes<?> bytes = Bytes.wrapForRead(payload).unchecked(true);
        assertEquals(4, bytes.readInt(1));
        bytes.readLimit(1);
        return bytes;
    }

    private static Bytes<?> stringAnyWithTruncatedStopBitPrefix() {
        Bytes<?> bytes = stringAnyPayload().unchecked(true);
        bytes.readLimit(1);
        return bytes;
    }

    private static Bytes<?> stringAnyWithBodyBeyondCurrentReadLimit() {
        Bytes<?> bytes = stringAnyPayload().unchecked(true);
        bytes.readLimit(2);
        return bytes;
    }

    private static Bytes<?> stringAnyBodyWithBodyBeyondCurrentReadLimit() {
        byte[] payload = {
                0x04,
                't', 'e', 's', 't',
                (byte) BinaryWireCode.TRUE
        };
        Bytes<?> bytes = Bytes.wrapForRead(payload).unchecked(true);
        bytes.readLimit(1);
        return bytes;
    }

    private static Bytes<?> validStringAny() {
        return stringAnyPayload();
    }

    private static Bytes<?> stringAnyPayload() {
        byte[] payload = {
                (byte) BinaryWireCode.STRING_ANY,
                0x04,
                't', 'e', 's', 't',
                (byte) BinaryWireCode.TRUE
        };
        return Bytes.wrapForRead(payload);
    }

    private static Bytes<?> stringAnyNull() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        bytes.writeByte((byte) BinaryWireCode.STRING_ANY);
        bytes.writeUtf8((String) null);
        bytes.readPositionRemaining(0, bytes.writePosition());
        return bytes;
    }

    private static Bytes<?> compactStringWithBodyBeyondCurrentReadLimit() {
        Bytes<?> bytes = compactStringPayload().unchecked(true);
        bytes.readLimit(1);
        return bytes;
    }

    private static Bytes<?> validCompactString() {
        return compactStringPayload();
    }

    private static Bytes<?> compactStringPayload() {
        byte[] payload = {
                (byte) (BinaryWireCode.STRING_0 + 4),
                't', 'e', 's', 't',
                (byte) BinaryWireCode.TRUE
        };
        return Bytes.wrapForRead(payload);
    }

    private static Bytes<?> uncheckedLength32U8ArrayWithZeroPayloadLength() {
        byte[] payload = {
                (byte) BinaryWireCode.BYTES_LENGTH32,
                0x00, 0x00, 0x00, 0x00,
                (byte) BinaryWireCode.U8_ARRAY
        };
        Bytes<?> bytes = Bytes.wrapForRead(payload).unchecked(true);
        assertEquals(0, bytes.readInt(1));
        bytes.readLimit(6);
        return bytes;
    }

    private static Bytes<?> uncheckedLength32WithZeroDeclaredBody() {
        byte[] payload = {
                (byte) BinaryWireCode.BYTES_LENGTH32,
                0x00, 0x00, 0x00, 0x00,
                (byte) BinaryWireCode.U8_ARRAY,
                0x55
        };
        Bytes<?> bytes = Bytes.wrapForRead(payload).unchecked(true);
        assertEquals(0, bytes.readInt(1));
        bytes.readLimit(5);
        return bytes;
    }

    private static Bytes<?> length32BodyWithNarrowReadLimit(byte bodyCode) {
        byte[] payload = {
                (byte) BinaryWireCode.BYTES_LENGTH32,
                0x04, 0x00, 0x00, 0x00,
                bodyCode,
                0x01, 0x02, 0x03
        };
        Bytes<?> bytes = Bytes.wrapForRead(payload).unchecked(true);
        assertEquals(4, bytes.readInt(1));
        bytes.readLimit(6);
        return bytes;
    }

    private static void assertReadableStateAndPayload(Bytes<?> bytes, long expectedPosition) {
        assertEquals(expectedPosition, bytes.readPosition());
        assertEquals(6, bytes.readLimit());
        assertTrue(bytes.readRemaining() >= 0);
        assertTrue(bytes.readPosition() <= bytes.readLimit());
        assertEquals(BinaryWireCode.U8_ARRAY, bytes.readUnsignedByte(5));
        assertEquals(0x01, bytes.readUnsignedByte(6));
        assertEquals(0x02, bytes.readUnsignedByte(7));
        assertEquals(0x03, bytes.readUnsignedByte(8));
    }

    private static void assertLength32BodyState(Bytes<?> bytes, long expectedPosition, int expectedBodyCode) {
        assertEquals(expectedPosition, bytes.readPosition());
        assertEquals(6, bytes.readLimit());
        assertTrue(bytes.readRemaining() >= 0);
        assertTrue(bytes.readPosition() <= bytes.readLimit());
        assertEquals(expectedBodyCode, bytes.readUnsignedByte(5));
        assertEquals(0x01, bytes.readUnsignedByte(6));
        assertEquals(0x02, bytes.readUnsignedByte(7));
        assertEquals(0x03, bytes.readUnsignedByte(8));
    }

    private static void assertZeroLengthBodyState(Bytes<?> bytes) {
        assertEquals(5, bytes.readPosition());
        assertEquals(5, bytes.readLimit());
        assertEquals(0, bytes.readRemaining());
        assertTrue(bytes.readPosition() <= bytes.readLimit());
        assertEquals(BinaryWireCode.U8_ARRAY, bytes.readUnsignedByte(5));
        assertEquals(0x55, bytes.readUnsignedByte(6));
    }

    private static void assertTruncatedPadding32PrefixState(Bytes<?> bytes) {
        assertEquals(1, bytes.readPosition());
        assertEquals(1, bytes.readLimit());
        assertEquals(0, bytes.readRemaining());
        assertTrue(bytes.readPosition() <= bytes.readLimit());
        assertEquals(BinaryWireCode.PADDING32, bytes.readUnsignedByte(0));
        assertEquals(0x04, bytes.readUnsignedByte(1));
        assertEquals(0x00, bytes.readUnsignedByte(2));
        assertEquals(0x00, bytes.readUnsignedByte(3));
        assertEquals(0x00, bytes.readUnsignedByte(4));
    }

    private static void assertTruncatedPadding32BodyPrefixState(Bytes<?> bytes) {
        assertEquals(0, bytes.readPosition());
        assertEquals(0, bytes.readLimit());
        assertEquals(0, bytes.readRemaining());
        assertTrue(bytes.readPosition() <= bytes.readLimit());
        assertEquals(0x04, bytes.readUnsignedByte(0));
        assertEquals(0x00, bytes.readUnsignedByte(1));
        assertEquals(0x00, bytes.readUnsignedByte(2));
        assertEquals(0x00, bytes.readUnsignedByte(3));
    }

    private static void assertTruncatedLength8PrefixState(Bytes<?> bytes) {
        assertEquals(1, bytes.readPosition());
        assertEquals(1, bytes.readLimit());
        assertEquals(0, bytes.readRemaining());
        assertTrue(bytes.readPosition() <= bytes.readLimit());
        assertEquals(BinaryWireCode.BYTES_LENGTH8, bytes.readUnsignedByte(0));
        assertEquals(0x04, bytes.readUnsignedByte(1));
        assertEquals(BinaryWireCode.U8_ARRAY, bytes.readUnsignedByte(2));
        assertEquals(0x01, bytes.readUnsignedByte(3));
        assertEquals(0x02, bytes.readUnsignedByte(4));
        assertEquals(0x03, bytes.readUnsignedByte(5));
    }

    private static void assertTruncatedLength16PrefixState(Bytes<?> bytes) {
        assertEquals(1, bytes.readPosition());
        assertEquals(1, bytes.readLimit());
        assertEquals(0, bytes.readRemaining());
        assertTrue(bytes.readPosition() <= bytes.readLimit());
        assertEquals(BinaryWireCode.BYTES_LENGTH16, bytes.readUnsignedByte(0));
        assertEquals(0x04, bytes.readUnsignedByte(1));
        assertEquals(0x00, bytes.readUnsignedByte(2));
        assertEquals(BinaryWireCode.U8_ARRAY, bytes.readUnsignedByte(3));
        assertEquals(0x01, bytes.readUnsignedByte(4));
        assertEquals(0x02, bytes.readUnsignedByte(5));
        assertEquals(0x03, bytes.readUnsignedByte(6));
    }

    private static void assertTruncatedLength32PrefixState(Bytes<?> bytes) {
        assertEquals(1, bytes.readPosition());
        assertEquals(1, bytes.readLimit());
        assertEquals(0, bytes.readRemaining());
        assertTrue(bytes.readPosition() <= bytes.readLimit());
        assertEquals(BinaryWireCode.BYTES_LENGTH32, bytes.readUnsignedByte(0));
        assertEquals(0x04, bytes.readUnsignedByte(1));
        assertEquals(0x00, bytes.readUnsignedByte(2));
        assertEquals(0x00, bytes.readUnsignedByte(3));
        assertEquals(0x00, bytes.readUnsignedByte(4));
        assertEquals(BinaryWireCode.U8_ARRAY, bytes.readUnsignedByte(5));
        assertEquals(0x01, bytes.readUnsignedByte(6));
        assertEquals(0x02, bytes.readUnsignedByte(7));
        assertEquals(0x03, bytes.readUnsignedByte(8));
    }

    private static void assertTruncatedTypePrefixStopBitState(Bytes<?> bytes) {
        assertEquals(1, bytes.readPosition());
        assertEquals(1, bytes.readLimit());
        assertEquals(0, bytes.readRemaining());
        assertTrue(bytes.readPosition() <= bytes.readLimit());
        assertEquals(BinaryWireCode.TYPE_PREFIX, bytes.readUnsignedByte(0));
        assertEquals(0x04, bytes.readUnsignedByte(1));
        assertEquals('t', bytes.readUnsignedByte(2));
        assertEquals('y', bytes.readUnsignedByte(3));
        assertEquals('p', bytes.readUnsignedByte(4));
        assertEquals('e', bytes.readUnsignedByte(5));
        assertEquals(BinaryWireCode.TRUE, bytes.readUnsignedByte(6));
    }

    private static void assertStringAnyState(Bytes<?> bytes, long expectedPosition, long expectedLimit) {
        assertEquals(expectedPosition, bytes.readPosition());
        assertEquals(expectedLimit, bytes.readLimit());
        assertTrue(bytes.readPosition() <= bytes.readLimit());
        assertStringAnyBytes(bytes);
    }

    private static void assertStringAnyBytes(Bytes<?> bytes) {
        assertEquals(BinaryWireCode.STRING_ANY, bytes.readUnsignedByte(0));
        assertEquals(0x04, bytes.readUnsignedByte(1));
        assertEquals('t', bytes.readUnsignedByte(2));
        assertEquals('e', bytes.readUnsignedByte(3));
        assertEquals('s', bytes.readUnsignedByte(4));
        assertEquals('t', bytes.readUnsignedByte(5));
        assertEquals(BinaryWireCode.TRUE, bytes.readUnsignedByte(6));
    }

    private static void assertStringAnyBodyState(Bytes<?> bytes, long expectedPosition, long expectedLimit) {
        assertEquals(expectedPosition, bytes.readPosition());
        assertEquals(expectedLimit, bytes.readLimit());
        assertTrue(bytes.readPosition() <= bytes.readLimit());
        assertEquals(0x04, bytes.readUnsignedByte(0));
        assertEquals('t', bytes.readUnsignedByte(1));
        assertEquals('e', bytes.readUnsignedByte(2));
        assertEquals('s', bytes.readUnsignedByte(3));
        assertEquals('t', bytes.readUnsignedByte(4));
        assertEquals(BinaryWireCode.TRUE, bytes.readUnsignedByte(5));
    }

    private static void assertCompactStringState(Bytes<?> bytes, long expectedPosition, long expectedLimit) {
        assertEquals(expectedPosition, bytes.readPosition());
        assertEquals(expectedLimit, bytes.readLimit());
        assertTrue(bytes.readPosition() <= bytes.readLimit());
        assertEquals(BinaryWireCode.STRING_0 + 4, bytes.readUnsignedByte(0));
        assertEquals('t', bytes.readUnsignedByte(1));
        assertEquals('e', bytes.readUnsignedByte(2));
        assertEquals('s', bytes.readUnsignedByte(3));
        assertEquals('t', bytes.readUnsignedByte(4));
        assertEquals(BinaryWireCode.TRUE, bytes.readUnsignedByte(5));
    }

    private static void assertTrailingPaddingAndTrue(Bytes<?> bytes) {
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

    private static void assertTrailingTypePrefixBytes(Bytes<?> bytes) {
        assertEquals(BinaryWireCode.TYPE_PREFIX, bytes.readUnsignedByte(2));
        assertEquals(0x03, bytes.readUnsignedByte(3));
        assertEquals('x', bytes.readUnsignedByte(4));
        assertEquals('x', bytes.readUnsignedByte(5));
        assertEquals('x', bytes.readUnsignedByte(6));
        assertEquals(BinaryWireCode.U8_ARRAY, bytes.readUnsignedByte(7));
        assertEquals(0x55, bytes.readUnsignedByte(8));
    }

    @Test
    public void copyOneStillCopiesValidBytesLength32Sequence() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        BinaryWire writer = new BinaryWire(bytes);
        writer.getValueOut().sequence(v -> {
            v.text("ok");
            v.int32(7);
        });

        assertEquals(BinaryWireCode.BYTES_LENGTH32, bytes.readUnsignedByte(0));

        bytes.readPositionRemaining(0, bytes.writePosition());
        Bytes<?> outputBytes = Bytes.allocateElasticOnHeap();
        Wire outputWire = WireType.TEXT.apply(outputBytes);

        new BinaryWire(bytes).copyOne(outputWire);

        String output = outputBytes.toString();
        assertTrue(output.contains("ok"));
        assertTrue(output.contains("7"));
    }

    private static byte[] negativeBytesLength32Payload() {
        return new byte[]{
                (byte) BinaryWireCode.BYTES_LENGTH32,
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
        };
    }
}
