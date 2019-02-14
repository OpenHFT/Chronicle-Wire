/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.core.annotation.NotNull;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("rawtypes")
public class WireInternalTest {

    @Test
    public void testThrowableAsObject() {
        final Bytes bytes = Bytes.elasticByteBuffer();
        try {
            final Wire wire = new BinaryWire(bytes);

            final Exception exc = new Exception();

            wire.write(() -> "exc").object(exc);

            final Throwable actual = (Throwable) wire.read("exc").object();
            StackTraceElement[] expectedST = exc.getStackTrace();
            StackTraceElement[] actualST = actual.getStackTrace();
            assertEquals(expectedST.length, actualST.length);
        } finally {
            bytes.release();
        }
    }

    @Test
    public void testThrowable() {
        final Bytes bytes = Bytes.elasticByteBuffer();
        try {
            final Wire wire = new TextWire(bytes);

            final Exception exc = new Exception();

            wire.write(() -> "exc").throwable(exc);

            final Throwable actual = wire.read("exc").throwable(false);
            StackTraceElement[] expectedST = exc.getStackTrace();
            StackTraceElement[] actualST = actual.getStackTrace();
            assertEquals(expectedST.length, actualST.length);
        } finally {
            bytes.release();
        }
    }

    @Test
    public void testFromSizePrefixedBinaryToText() {
        Bytes bytes = Bytes.elasticByteBuffer();
        @NotNull Wire out = new BinaryWire(bytes);
        out.writeDocument(true, w -> w
                .write(() -> "csp").text("csp://hello-world")
                .write(() -> "tid").int64(123456789));
        out.writeDocument(false, w -> w.write(() -> "reply").marshallable(
                w2 -> w2.write(() -> "key").int16(1)
                        .write(() -> "value").text("Hello World")));
        out.writeDocument(false, w -> w.write(() -> "reply").sequence(
                w2 -> {
                    w2.text("key");
                    w2.int16(2);
                    w2.text("value");
                    w2.text("Hello World2");
                }));
        out.writeDocument(false, wireOut -> wireOut.writeEventName(() -> "userid").text("peter"));

        String actual = Wires.fromSizePrefixedBlobs(bytes);
        assertEquals("--- !!meta-data #binary\n" +
                "csp: \"csp://hello-world\"\n" +
                "tid: !int 123456789\n" +
                "# position: 35, header: 0\n" +
                "--- !!data #binary\n" +
                "reply: {\n" +
                "  key: 1,\n" +
                "  value: Hello World\n" +
                "}\n" +
                "# position: 73, header: 1\n" +
                "--- !!data #binary\n" +
                "reply: [\n" +
                "  key,\n" +
                "  2,\n" +
                "  value,\n" +
                "  Hello World2\n" +
                "]\n" +
                "# position: 112, header: 2\n" +
                "--- !!data #binary\n" +
                "userid: peter\n", actual);

        bytes.release();
    }

    @After
    public void checkRegisteredBytes() {
        BytesUtil.checkRegisteredBytes();
    }
}