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
import net.openhft.chronicle.bytes.util.BinaryLengthLength;
import net.openhft.chronicle.core.io.IORuntimeException;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/**
 * The 'DummyDataSmall' class extends 'DummyData', providing specific implementations
 * of read and write marshalling methods, tailored for scenarios where data lengths
 * are expected to be small and can be represented with an unsigned byte.
 */
public class DummyDataSmall extends DummyData {

    /**
     * Overrides the 'readMarshallable' method to define how an instance of 'DummyDataSmall'
     * should be read from binary data. It assumes that the length of 'data' can
     * be represented with an unsigned byte, which is suitable for small byte arrays.
     *
     * @param bytes A 'BytesIn' instance which provides the binary data to be read.
     * @throws IORuntimeException, BufferUnderflowException, IllegalStateException Potential exceptions during reading operation.
     */
    @Override
    public void readMarshallable(BytesIn bytes) throws IORuntimeException, BufferUnderflowException, IllegalStateException {
        timeNS = bytes.readLong(); // Reading a long value (timeNS)
        int len = bytes.readUnsignedByte(); // Reading length of data as an unsigned byte
        if ((byte) len == -1) {
            data = null;
        } else {
            if (data == null || data.length != len)
                data = new byte[len]; // Initializing or re-sizing 'data' array
            bytes.read(data); // Reading byte data
        }
    }

    /**
     * Overrides the 'writeMarshallable' method to define how an instance of 'DummyDataSmall'
     * should be written to binary data. It assumes that the length of 'data' can
     * be represented with an unsigned byte, which is suitable for small byte arrays.
     *
     * @param bytes A 'BytesOut' instance which will receive the binary data to be written.
     * @throws IllegalStateException, BufferOverflowException, BufferUnderflowException, ArithmeticException Potential exceptions during writing operation.
     */
    @Override
    public void writeMarshallable(BytesOut bytes) throws IllegalStateException, BufferOverflowException, BufferUnderflowException, ArithmeticException {
        bytes.writeLong(timeNS); // Writing the nanosecond timestamp
        if (data == null) {
            bytes.writeUnsignedByte(-1); // Writing a null flag if data is null
        } else {
            bytes.writeUnsignedByte(data.length); // Writing the length of the data using an unsigned byte
            bytes.write(data); // Writing the actual byte data
        }
    }

    /**
     * Overrides the 'binaryLengthLength' method to declare the bit length used
     * for expressing the length of binary data, which in this case is set to be 8-bit.
     * This method informs marshallers/unmarshallers about the expected bit length
     * of the length field for binary encoded data.
     *
     * @return BinaryLengthLength Enum value indicating that an 8-bit length field should be used.
     */
    @Override
    public BinaryLengthLength binaryLengthLength() {
        return BinaryLengthLength.LENGTH_8BIT;
    }
}
