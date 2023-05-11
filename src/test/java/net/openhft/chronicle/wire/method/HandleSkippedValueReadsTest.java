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

package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.Mocker;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@RunWith(Parameterized.class)
public class HandleSkippedValueReadsTest {

    final WireType wireType;

    public HandleSkippedValueReadsTest(WireType wireType) {
        this.wireType = wireType;
    }

    @Parameterized.Parameters(name = "wireType={0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[]{WireType.BINARY_LIGHT},
                new Object[]{WireType.TEXT}
                // TODO FIX
//                new Object[]{WireType.YAML}
        );
    }

    @Test
    public void test() {
        Wire wire = wireType.apply(Bytes.allocateElasticOnHeap());
        try (DocumentContext dc = wire.writingDocument(true)) {
            dc.wire()
                    .write("meta").text("one")
                    .write("prefix").object(WireType.BINARY_LIGHT) // dropped
                    .write("meta").text("oneB");
        }

        try (DocumentContext dc = wire.writingDocument(true)) {
            dc.wire()
                    .write("other").text("two") // skipped
                    .write("meta").text("two"); // dropped
        }

        try (DocumentContext dc = wire.writingDocument(true)) {
            dc.wire().write("meta").text("three");
        }

        try (DocumentContext dc = wire.writingDocument()) {
            dc.wire()
                    .write("data").text("four")
                    .write("prefix").object(WireType.BINARY_LIGHT)
                    .write("data").text("fourB"); // dropped
        }

        try (DocumentContext dc = wire.writingDocument()) {
            dc.wire()
                    .write("other").text("five") // skipped
                    .write("data").text("five"); // dropped
        }

        try (DocumentContext dc = wire.writingDocument()) {
            dc.wire().write("data").text("six");
        }

        StringWriter sw = new StringWriter();
        final MethodReader reader = wire.methodReaderBuilder()
                .metaDataHandler(Mocker.logging(MetaMethod.class, "M ", sw))
                .build(Mocker.logging(DataMethod.class, "D ", sw));

        assertTrue(reader.readOne());
        assertEquals("M meta[one]\n" +
                        "M meta[three]\n" +
                        "D data[four]\n" +
                        "D data[fourB]\n",
                sw.toString().replace("\r", ""));
        assertTrue(reader.readOne());
        assertEquals("M meta[one]\n" +
                "M meta[three]\n" +
                "D data[four]\n" +
                "D data[fourB]\n" +
                "D data[five]\n",
                sw.toString().replace("\r", ""));
        assertTrue(reader.readOne());
        assertEquals("M meta[one]\n" +
                "M meta[three]\n" +
                "D data[four]\n" +
                "D data[fourB]\n" +
                "D data[five]\n" +
                "D data[six]\n",
                sw.toString().replace("\r", ""));
        assertFalse(reader.readOne());
    }

    @Test
    public void index2index() {
        Wire wire = wireType.apply(Bytes.allocateElasticOnHeap());
        try (DocumentContext dc = wire.writingDocument(true)) {
            dc.wire()
                    .write("meta").text("one");
        }
        try (DocumentContext dc = wire.writingDocument(true)) {
            dc.wire()
                    .write("index2index").int64array(32);
        }
        try (DocumentContext dc = wire.writingDocument()) {
            dc.wire().write("data").text("six");
        }

        StringWriter sw = new StringWriter();
        final MethodReader reader = wire.methodReaderBuilder()
                .metaDataHandler(Mocker.logging(MetaMethod.class, "M ", sw))
                .build(Mocker.logging(DataMethod.class, "D ", sw));

        assertTrue(reader.readOne());
        assertEquals("" +
                        "M meta[one]\n" +
                        "D data[six]\n"
                , sw.toString().replace("\r", ""));
        assertFalse(reader.readOne());

    }

    interface MetaMethod {
        void meta(String text);
    }

    interface DataMethod {
        void data(String text);
    }
}
