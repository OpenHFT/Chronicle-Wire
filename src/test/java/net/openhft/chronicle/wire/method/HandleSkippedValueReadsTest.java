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
import org.jetbrains.annotations.NotNull;
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
//                new Object[]{WireType.YAML_ONLY}
        );
    }

    @Test
    public void test() {
        doTest(false);
    }

    @Test
    public void testScanning() {
        doTest(true);
    }

    public void doTest(boolean scanning) {
        Wire wire = wireType.apply(Bytes.allocateElasticOnHeap());
        try (DocumentContext dc = wire.writingDocument(true)) {
            dc.wire()
                    .write("meta").text("one")
                    .write("prefix").object(WireType.BINARY_LIGHT) // skipped
                    .write("meta").text("oneB");
        }

        try (DocumentContext dc = wire.writingDocument(true)) {
            dc.wire()
                    .write("other").text("two") // skipped
                    .write("meta").text("two");
        }

        try (DocumentContext dc = wire.writingDocument(true)) {
            dc.wire().write("meta").text("three");
        }

        try (DocumentContext dc = wire.writingDocument()) {
            dc.wire()
                    .write("data").text("four")
                    .write("prefix").object(WireType.BINARY_LIGHT) // skipped
                    .write("data").text("fourB");
        }

        try (DocumentContext dc = wire.writingDocument()) {
            dc.wire()
                    .write("other").text("five") // skipped
                    .write("data").text("five");
        }

        try (DocumentContext dc = wire.writingDocument()) {
            dc.wire().write("data").text("six");
        }

        StringWriter sw = new StringWriter();
        final MethodReader reader = wire.methodReaderBuilder()
                .scanning(scanning)
                .metaDataHandler(Mocker.logging(MetaMethod.class, "M ", sw))
                .build(Mocker.logging(DataMethod.class, "D ", sw));

        if (scanning) {
            assertTrue(reader.readOne());
            assertEquals("" +
                            "M meta[one]\n" +
                            "M meta[oneB]\n" +
                            "M meta[two]\n" +
                            "M meta[three]\n" +
                            "D data[four]\n" +
                            "D data[fourB]\n",
                    asString(sw));

        } else {
            // one
            assertTrue(reader.readOne());
            assertEquals("" +
                            "M meta[one]\n" +
                            "M meta[oneB]\n",
                    asString(sw));
            // two
            assertTrue(reader.readOne());
            assertEquals("" +
                            "M meta[one]\n" +
                            "M meta[oneB]\n" +
                            "M meta[two]\n",
                    asString(sw));
            // three
            assertTrue(reader.readOne());
            assertEquals("" +
                            "M meta[one]\n" +
                            "M meta[oneB]\n" +
                            "M meta[two]\n" +
                            "M meta[three]\n",
                    asString(sw));
            // four
            assertTrue(reader.readOne());
            assertEquals("" +
                            "M meta[one]\n" +
                            "M meta[oneB]\n" +
                            "M meta[two]\n" +
                            "M meta[three]\n" +
                            "D data[four]\n" +
                            "D data[fourB]\n",
                    asString(sw));
        }
        // five
        assertTrue(reader.readOne());
        assertEquals("" +
                        "M meta[one]\n" +
                        "M meta[oneB]\n" +
                        "M meta[two]\n" +
                        "M meta[three]\n" +
                        "D data[four]\n" +
                        "D data[fourB]\n" +
                        "D data[five]\n",
                asString(sw));
        // six
        assertTrue(reader.readOne());
        assertEquals("" +
                        "M meta[one]\n" +
                        "M meta[oneB]\n" +
                        "M meta[two]\n" +
                        "M meta[three]\n" +
                        "D data[four]\n" +
                        "D data[fourB]\n" +
                        "D data[five]\n" +
                        "D data[six]\n",
                asString(sw));
        assertFalse(reader.readOne());
    }

    @NotNull
    private static String asString(StringWriter sw) {
        return sw.toString().replace("\r", "");
    }

    @Test
    public void index2index() {
        doIndex2index(false);
    }

    @Test
    public void index2indexScanning() {
        doIndex2index(true);
    }

    public void doIndex2index(boolean scanning) {
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
                .scanning(scanning)
                .metaDataHandler(Mocker.logging(MetaMethod.class, "M ", sw))
                .build(Mocker.logging(DataMethod.class, "D ", sw));

        assertTrue(reader.readOne());

        if (!scanning) {
            // one
            assertEquals("" +
                            "M meta[one]\n"
                    , asString(sw));
            assertTrue(reader.readOne());
            // i2i
            assertEquals("" +
                            "M meta[one]\n"
                    , asString(sw));
            assertTrue(reader.readOne());
        }
        // data six
        assertEquals("" +
                        "M meta[one]\n" +
                        "D data[six]\n"
                , asString(sw));
        assertFalse(reader.readOne());
    }

    interface MetaMethod {
        void meta(String text);
    }

    interface DataMethod {
        void data(String text);
    }
}
