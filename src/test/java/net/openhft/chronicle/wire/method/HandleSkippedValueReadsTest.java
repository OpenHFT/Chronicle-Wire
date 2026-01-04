/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.util.Mocker;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class HandleSkippedValueReadsTest extends net.openhft.chronicle.wire.WireTestCommon {

    private WireType wireType;

    void initHandleSkippedValueReadsTest(WireType wireType) {
        this.wireType = wireType;
    }

    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[]{WireType.BINARY_LIGHT},
                new Object[]{WireType.TEXT}
        );
    }

    @NotNull
    private static String asString(StringWriter sw) {
        return sw.toString().replace("\r", "");
    }

    @MethodSource("data")
    @DisplayName("Handle skipped value reads in metadata and data")
    @ParameterizedTest(name = "wire type {0} handles skipped values without scanning")
    void test(WireType wireType) {
        initHandleSkippedValueReadsTest(wireType);
        String output = doTest(false);
        assertTrue(output.contains("M meta[one]"),
                output + " should contain M meta[one] in non-scanning mode");
    }

    @MethodSource("data")
    @DisplayName("Handle skipped value reads with scanning enabled")
    @ParameterizedTest(name = "wire type {0} handles skipped values with scanning")
    void testScanning(WireType wireType) {
        initHandleSkippedValueReadsTest(wireType);
        String output = doTest(true);
        assertTrue(output.contains("M meta[one]"),
                output + " should contain M meta[one] in scanning mode");
    }

    private String doTest(boolean scanning) {
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
            assertTrue(reader.readOne(), "scanning should read first metadata document");
            assertEquals("M meta[one]\n" +
                            "M meta[oneB]\n" +
                            "M meta[two]\n" +
                            "M meta[three]\n" +
                            "D data[four]\n" +
                            "D data[fourB]\n",
                    asString(sw), "scanning should accumulate expected metadata and data messages");

        } else {
            // one
            assertTrue(reader.readOne(), "non-scanning should read first metadata document");
            assertEquals("M meta[one]\n" +
                            "M meta[oneB]\n",
                    asString(sw), "non-scanning should log first metadata messages");
            // two
            assertTrue(reader.readOne(), "non-scanning should read second metadata document");
            assertEquals("M meta[one]\n" +
                            "M meta[oneB]\n" +
                            "M meta[two]\n",
                    asString(sw), "non-scanning should include meta[two] after second read");
            // three
            assertTrue(reader.readOne(), "non-scanning should read third metadata document");
            assertEquals("M meta[one]\n" +
                            "M meta[oneB]\n" +
                            "M meta[two]\n" +
                            "M meta[three]\n",
                    asString(sw), "non-scanning should include meta[three] after third read");
            // four
            assertTrue(reader.readOne(), "non-scanning should read first data document");
            assertEquals("M meta[one]\n" +
                            "M meta[oneB]\n" +
                            "M meta[two]\n" +
                            "M meta[three]\n" +
                            "D data[four]\n" +
                            "D data[fourB]\n",
                    asString(sw), "non-scanning should include first data messages after fourth read");
        }
        // five
        assertTrue(reader.readOne(), "reader should read second data document");
        assertEquals("M meta[one]\n" +
                        "M meta[oneB]\n" +
                        "M meta[two]\n" +
                        "M meta[three]\n" +
                        "D data[four]\n" +
                        "D data[fourB]\n" +
                        "D data[five]\n",
                asString(sw), "log should include data[five] after next read");
        // six
        assertTrue(reader.readOne(), "reader should read final data document");
        assertEquals("M meta[one]\n" +
                        "M meta[oneB]\n" +
                        "M meta[two]\n" +
                        "M meta[three]\n" +
                        "D data[four]\n" +
                        "D data[fourB]\n" +
                        "D data[five]\n" +
                        "D data[six]\n",
                asString(sw), "log should include data[six] after final read");
        assertFalse(reader.readOne(), "no more documents should remain after reading six");
        return asString(sw);
    }

    @MethodSource("data")
    @DisplayName("Handle skipped value reads for index2index metadata")
    @ParameterizedTest(name = "wire type {0} handles index2index without scanning")
    void index2index(WireType wireType) {
        initHandleSkippedValueReadsTest(wireType);
        String output = doIndex2index(false);
        assertTrue(output.contains("M meta[one]"),
                output + " should contain M meta[one] in non-scanning index2index");
    }

    @MethodSource("data")
    @DisplayName("Handle skipped value reads for index2index with scanning")
    @ParameterizedTest(name = "wire type {0} handles index2index with scanning")
    void index2indexScanning(WireType wireType) {
        initHandleSkippedValueReadsTest(wireType);
        String output = doIndex2index(true);
        assertTrue(output.contains("M meta[one]"),
                output + " should contain M meta[one] in scanning index2index");
    }

    private String doIndex2index(boolean scanning) {
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

        assertTrue(reader.readOne(), "index2index should read first metadata document");

        if (!scanning) {
            // one
            assertEquals("M meta[one]\n", asString(sw),
                    "non-scanning should log meta[one] after first read");
            assertTrue(reader.readOne(), "index2index should read index2index document");
            // i2i
            assertEquals("M meta[one]\n", asString(sw),
                    "index2index should not add output for skipped index2index entry");
            assertTrue(reader.readOne(), "index2index should read data document");
        }
        // data six
        assertEquals("M meta[one]\n" +
                        "D data[six]\n",
                asString(sw), "final log should include data[six] after read");
        assertFalse(reader.readOne(), "no more documents should remain after data[six]");
        return asString(sw);
    }

    interface MetaMethod {
        void meta(String text);
    }

    interface DataMethod {
        void data(String text);
    }
}
