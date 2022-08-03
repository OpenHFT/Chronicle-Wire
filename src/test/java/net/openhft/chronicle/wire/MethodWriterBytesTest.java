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

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class MethodWriterBytesTest {
    private ArrayBlockingQueue<Bytes> q = new ArrayBlockingQueue(1);

    interface Print {
        void msg(Bytes message);
    }

    @Test
    public void test() throws InterruptedException {
        Wire w = new BinaryWire(Bytes.allocateElasticOnHeap());
        Print printer = w.methodWriter(Print.class);
        printer.msg(Bytes.from("hello"));

        MethodReader reader = w.methodReader((Print) this::println);
        reader.readOne();

        Bytes result = q.poll(10, TimeUnit.SECONDS);
        Assert.assertEquals("hello", result.toString());
    }

    private void println(Bytes bytes) {
        q.add(bytes);
    }

}
