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

package net.openhft.chronicle.wire.channel.internal;

import net.openhft.chronicle.bytes.*;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.*;

// Extends `BytesInBinaryMarshallable` which likely facilitates efficient serialization/deserialization
// of this object into/from binary format (such as Bytes).
public class ChronicleEvent extends BytesInBinaryMarshallable {

    // Static fields that are related to the class itself, not to an instance.
    // They seem to be used for efficient marshalling and copying.
    static final int START_BYTES = BytesUtil.triviallyCopyableStart(ChronicleEvent.class);
    static final int LENGTH_BYTES = BytesUtil.triviallyCopyableLength(ChronicleEvent.class);
    static int count = 0; // might be used for debugging or some control mechanism.

    // Bytes fields for specific text groups.
    private final Bytes text3 = Bytes.forFieldGroup(this, "text3");
    private final Bytes text4 = Bytes.forFieldGroup(this, "text4");

    // Various fields for the event.
    private long sendingTimeNS;  // timestamp for when the event was sent.
    private long transactTimeNS; // timestamp for when the event was processed.

    // Fields with converters, likely to change the format of the long values.
    @LongConversion(NanoTimestampLongConverter.class)
    private long dateTime1, dateTime2, dateTime3, dateTime4;
    @LongConversion(Base85LongConverter.class)
    private long text1, text2; // up to 9 ASCII chars
    @FieldGroup("text3")
    private long text3a, text3b, text3c;
    @FieldGroup("text4")
    private long text4a, text4b, text4c, text4d, text4e;

    // Numeric values for the event.
    private int number1, number2;
    private long number3, number4;
    private double value1, value2, value3, value4, value5, value6, value7, value8;

    // Methods to read and write the object from/into Bytes, utilizing unsafe operations for efficiency.
    @Override
    public final void readMarshallable(BytesIn bytes) throws IORuntimeException {
        bytes.unsafeReadObject(this, START_BYTES, LENGTH_BYTES);
    }

    @Override
    public final void writeMarshallable(BytesOut bytes) {
        bytes.unsafeWriteObject(this, START_BYTES, LENGTH_BYTES);

        // A control mechanism to slow down the producer during warmup, possibly for testing or debugging.
        if (bytes.writePosition() > (3 << 20)) {
            System.out.print('.');
            Jvm.pause(++count);
        }
    }

    // Getter and Setter for `transactTimeNS`.
    public void transactTimeNS(long transactTimeNS) {
        this.transactTimeNS = transactTimeNS;
    }

    public long transactTimeNS() {
        return transactTimeNS;
    }

    // Getter and Setter for `sendingTimeNS`.
    public void sendingTimeNS(long sendingTimeNS) {
        this.sendingTimeNS = sendingTimeNS;
    }

    public long sendingTimeNS() {
        return sendingTimeNS;
    }
}
