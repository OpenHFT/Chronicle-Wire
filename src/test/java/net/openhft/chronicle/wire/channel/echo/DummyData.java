/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire.channel.echo;

import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.BytesInBinaryMarshallable;
import net.openhft.chronicle.wire.converter.NanoTime;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/**
 * The 'DummyData' class extends 'BytesInBinaryMarshallable' to define a data structure
 * with the capabilities to read and write its properties in a binary format.
 * This class is designed to handle the marshalling and unmarshalling of its fields
 * 'timeNS' and 'data' into and from binary data for potential use in transmission or storage.
 */
public class DummyData extends BytesInBinaryMarshallable {
    @NanoTime
    long timeNS;
    byte[] data;

    /**
     * @return long Retriever for the 'timeNS' property.
     */
    public long timeNS() {
        return timeNS;
    }

    /**
     * Setter for the 'timeNS' property with a fluent API style return.
     *
     * @param timeNS the nanosecond timestamp to be set.
     * @return DummyData Returns 'this' to allow method chaining.
     */
    public DummyData timeNS(long timeNS) {
        this.timeNS = timeNS;
        return this;
    }

    /**
     * @return byte[] Retriever for the 'data' property.
     */
    public byte[] data() {
        return data;
    }

    /**
     * Setter for the 'data' property with a fluent API style return.
     *
     * @param data byte array to be set.
     * @return DummyData Returns 'this' to allow method chaining.
     */
    public DummyData data(byte[] data) {
        this.data = data;
        return this;
    }

    /**
     * Overrides the 'readMarshallable' method to define how an instance of 'DummyData'
     * should be read from binary data, handling both 'timeNS' and 'data' properties.
     *
     * @param bytes A 'BytesIn' instance which provides the binary data to be read.
     * @throws IORuntimeException, BufferUnderflowException, IllegalStateException Potential exceptions during reading operation.
     */
    @Override
    public void readMarshallable(BytesIn bytes) throws IORuntimeException, BufferUnderflowException, IllegalStateException {
        timeNS = bytes.readLong(); // Reading a long value (timeNS)
        int len = bytes.readInt();  // Reading length of the upcoming data byte array
        if (len == -1) {
            data = null;
        } else {
            if (data == null || data.length != len)
                data = new byte[len]; // Initializing or re-sizing 'data' array
            bytes.read(data); // Reading byte data
        }
    }

    /**
     * Overrides the 'writeMarshallable' method to define how an instance of 'DummyData'
     * should be written to binary data, handling both 'timeNS' and 'data' properties.
     *
     * @param bytes A 'BytesOut' instance which will receive the binary data to be written.
     * @throws IllegalStateException, BufferOverflowException, BufferUnderflowException, ArithmeticException Potential exceptions during writing operation.
     */
    @Override
    public void writeMarshallable(BytesOut bytes) throws IllegalStateException, BufferOverflowException, BufferUnderflowException, ArithmeticException {
        bytes.writeLong(timeNS); // Writing the nanosecond timestamp
        if (data == null) {
            bytes.writeInt(-1); // Writing a null flag if data is null
        } else {
            bytes.writeInt(data.length); // Writing the length of the data
            bytes.write(data); // Writing the actual byte data
        }
    }
}
