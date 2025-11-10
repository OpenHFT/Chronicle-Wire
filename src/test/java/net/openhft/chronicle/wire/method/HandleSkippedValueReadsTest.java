//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//
package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.util.Mocker;
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
public class HandleSkippedValueReadsTest extends net.openhft.chronicle.wire.WireTestCommon {

    private final WireType wireType;

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

    private void doTest(boolean scanning) {
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
                .exceptionHandlerOnUnknownMethod(Jvm.debug())
                .metaDataHandler(Mocker.logging(MetaMethod.class, "M ", sw))
                .build(Mocker.logging(DataMethod.class, "D ", sw));

        if (scanning) {
            assertTrue(reader.readOne());
            assertEquals("M meta[one]\n" +
                            "M meta[oneB]\n" +
                            "M meta[two]\n" +
                            "M meta[three]\n" +
                            "D data[four]\n" +
                            "D data[fourB]\n",
                    asString(sw));

        } else {
            // one
            assertTrue(reader.readOne());
            assertEquals("M meta[one]\n" +
                            "M meta[oneB]\n",
                    asString(sw));
            // two
            assertTrue(reader.readOne());
            assertEquals("M meta[one]\n" +
                            "M meta[oneB]\n" +
                            "M meta[two]\n",
                    asString(sw));
            // three
            assertTrue(reader.readOne());
            assertEquals("M meta[one]\n" +
                            "M meta[oneB]\n" +
                            "M meta[two]\n" +
                            "M meta[three]\n",
                    asString(sw));
            // four
            assertTrue(reader.readOne());
            assertEquals("M meta[one]\n" +
                            "M meta[oneB]\n" +
                            "M meta[two]\n" +
                            "M meta[three]\n" +
                            "D data[four]\n" +
                            "D data[fourB]\n",
                    asString(sw));
        }
        // five
        assertTrue(reader.readOne());
        assertEquals("M meta[one]\n" +
                        "M meta[oneB]\n" +
                        "M meta[two]\n" +
                        "M meta[three]\n" +
                        "D data[four]\n" +
                        "D data[fourB]\n" +
                        "D data[five]\n",
                asString(sw));
        // six
        assertTrue(reader.readOne());
        assertEquals("M meta[one]\n" +
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

    private void doIndex2index(boolean scanning) {
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
                .exceptionHandlerOnUnknownMethod(Jvm.debug())
                .metaDataHandler(Mocker.logging(MetaMethod.class, "M ", sw))
                .build(Mocker.logging(DataMethod.class, "D ", sw));

        assertTrue(reader.readOne());

        if (!scanning) {
            // one
            assertEquals("M meta[one]\n"
                    , asString(sw));
            assertTrue(reader.readOne());
            // i2i
            assertEquals("M meta[one]\n"
                    , asString(sw));
            assertTrue(reader.readOne());
        }
        // data six
        assertEquals("M meta[one]\n" +
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
