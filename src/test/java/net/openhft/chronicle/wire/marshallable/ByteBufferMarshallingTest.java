/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.util.ObjectUtils;
import net.openhft.chronicle.wire.RawWire;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.Test;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

/**
 * This class tests the marshalling and unmarshalling of objects using ByteBuffers.
 */
public class ByteBufferMarshallingTest extends WireTestCommon {

    /**
     * Test the write and read capabilities of a ByteBuffer.
     * It writes an instance of AClass to a Wire and then reads it back.
     * Finally, it asserts that the original and the read objects are the same.
     */
    @Test
    public void writeReadByteBuffer() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        // Initialize an elastic ByteBuffer and create a Wire for it
        Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        Wire wire = new RawWire(bytes);

        // Create an instance of AClass
        AClass o1 = new AClass(1, true, (byte) 2, '3', (short) 4, 5, 6, 7, 8, "nine");

        o1.writeMarshallable(wire);

        // Read the object back from the Wire
        AClass o2 = ObjectUtils.newInstance(AClass.class);
        o2.readMarshallable(wire);

        // Assert that the original and read objects are equal
        assertEquals(o1, o2);
        bytes.releaseLast();
    }

    /**
     * Test writing to a ByteBuffer and then reading via another ByteBuffer.
     * This test showcases the transition of data between two ByteBuffers.
     */
    @Test
    public void writeReadViaByteBuffer() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        // Initialize an elastic ByteBuffer and create a Wire for it
        Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        Wire wire = new RawWire(bytes);

        // Create an instance of AClass
        AClass o1 = new AClass(1, true, (byte) 2, '3', (short) 4, 5, 6, 7, 8, "nine");

        o1.writeMarshallable(wire);

        // Configure the positions and limits for the ByteBuffer
        ByteBuffer bb = bytes.underlyingObject();
        Buffer b = bb;
        b.position((int) bytes.readPosition());
        b.limit((int) bytes.readLimit());

        // Create a second ByteBuffer and transfer data from the first one
        Bytes<ByteBuffer> bytes2 = Bytes.elasticByteBuffer();
        bytes2.ensureCapacity(b.remaining());

        ByteBuffer bb2 = bytes2.underlyingObject();
        bb2.clear();

        bb2.put(bb);

        // Initialize reading positions for the second ByteBuffer
        bytes2.readPosition(0);
        bytes2.readLimit(bb2.position());

        // Create a new Wire for the second ByteBuffer and read the object from it
        Wire wire2 = new RawWire(bytes2);

        AClass o2 = ObjectUtils.newInstance(AClass.class);
        o2.readMarshallable(wire2);

        // Assert that the original and read objects are equal
        assertEquals(o1, o2);
        bytes.releaseLast();
        bytes2.releaseLast();
    }

    /**
     * Test writing an instance of BClass to Bytes and then reading it back via a ByteBuffer.
     * This test demonstrates the use of ByteBuffers without explicitly using a Wire.
     */
    @Test
    public void writeReadBytesViaByteBuffer() {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        // Initialize an elastic ByteBuffer
        Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();

        // Create an instance of BClass
        BClass o1 = new BClass(1, true, (byte) 2, '3', (short) 4, 5, 6, 7, 8, "nine");

        o1.writeMarshallable(bytes);

        // Configure the positions and limits for the ByteBuffer
        ByteBuffer bb = bytes.underlyingObject();
        bb.position((int) bytes.readPosition());
        bb.limit((int) bytes.readLimit());

        // Create a second ByteBuffer and transfer data from the first one
        Bytes<ByteBuffer> bytes2 = Bytes.elasticByteBuffer();
        bytes2.ensureCapacity(bb.remaining());

        ByteBuffer bb2 = bytes2.underlyingObject();
        bb2.clear();

        bb2.put(bb);

        // Initialize reading positions for the second ByteBuffer
        bytes2.readPosition(0);
        bytes2.readLimit(bb2.position());

        // Read the BClass object from the second ByteBuffer
        BClass o2 = ObjectUtils.newInstance(BClass.class);
        o2.readMarshallable(bytes2);

        // Assert that the original and read objects are equal
        assertEquals(o1, o2);
        bytes.releaseLast();
        bytes2.releaseLast();
    }
}
