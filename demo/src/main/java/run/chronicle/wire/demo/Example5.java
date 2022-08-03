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

package run.chronicle.wire.demo;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.BinaryWire;
import net.openhft.chronicle.wire.TextWire;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.Wires;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/**
 * Write a message with a thread safe size prefix.
 * The benefits of using this approach are that:
 * - The reader (tailer) is blocked until the message is completely written.
 * - If you have concurrent writers (appenders):
 * -- If the size of message is not known, other writers will be blocked until the message is written completely.
 * -- If the size of message is known, other writers will leave buffer space for this writer to complete writing
 * the message and concurrently write beyond the known size.
 */

public class Example5 {
    public static void main(String[] args) {

        // Bytes which wraps a ByteBuffer which is resized as needed.
        Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();

        Wire wire = new TextWire(bytes);
        ClassAliasPool.CLASS_ALIASES.addAlias(Data.class);
        Data data = new Data("Hello World", 1234567890L, TimeUnit.NANOSECONDS, 10.50);

        //writeDocument() blocks other readers and writers, until the writing of this
        //data is completed. See the above comment.
        wire.writeDocument(false, data);

        //Prints out:
/*      --- !!data
        message: Hello World
        number: 1234567890
        timeUnit: NANOSECONDS
        price: 10.5                                    */
        System.out.println(Wires.fromSizePrefixedBlobs(bytes));

        Data data2 = new Data();
        wire.readDocument(null, data2);

        //Prints out:
        /*  Data{message='Hello World', number=1234567890, timeUnit=NANOSECONDS, price=10.5}    */
        System.out.println(data2);

        //To write in binary instead:

        Bytes<ByteBuffer> bytes2 = Bytes.elasticByteBuffer();
        Wire wire2 = new BinaryWire(bytes2);
        wire2.writeDocument(false, data);

        //Prints out:
/*      --- !!data #binary
        message: Hello World
        number: !int 1234567890
        timeUnit: NANOSECONDS
        price: 10.5                        */
        System.out.println(Wires.fromSizePrefixedBlobs(bytes2));

        Data data3 = new Data();
        wire2.readDocument(null, data3);

        //Prints out:
        /*  Data{message='Hello World', number=1234567890, timeUnit=NANOSECONDS, price=10.5}   */
        System.out.println(data3);

        bytes.releaseLast();
        bytes2.releaseLast();
    }
}