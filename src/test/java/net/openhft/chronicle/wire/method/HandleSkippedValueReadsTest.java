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
                    .write("prefix").object(WireType.BINARY_LIGHT)
                    .write("meta").text("one");
        }

        try (DocumentContext dc = wire.writingDocument(true)) {
            dc.wire()
                    .write("other").text("two");
        }

        try (DocumentContext dc = wire.writingDocument(true)) {
            dc.wire().write("meta").text("three");
        }

        try (DocumentContext dc = wire.writingDocument()) {
            dc.wire()
                    .write("prefix").object(WireType.BINARY_LIGHT)
                    .write("data").text("four");
        }

        try (DocumentContext dc = wire.writingDocument()) {
            dc.wire()
                    .write("other").text("five");
        }

        try (DocumentContext dc = wire.writingDocument()) {
            dc.wire().write("data").text("six");
        }

        StringWriter sw = new StringWriter();
        final MethodReader reader = wire.methodReaderBuilder()
                .metaDataHandler(Mocker.logging(MetaMethod.class, "M ", sw))
                .build(Mocker.logging(DataMethod.class, "D ", sw));

        for (int i = 0; i < 6; i++) {
            reader.readOne();
        }
        assertEquals("M meta[one]\n" +
                "M meta[three]\n" +
                "D data[four]\n" +
                "D data[six]\n", sw.toString().replace("\r", ""));
    }

    @Test
    public void index2index() {
        Wire wire = wireType.apply(Bytes.allocateElasticOnHeap());
        try (DocumentContext dc = wire.writingDocument(true)) {
            dc.wire()
                    .write("index2index").int64array(32)
                    .write("meta").text("one");
        }

        StringWriter sw = new StringWriter();
        final MethodReader reader = wire.methodReaderBuilder()
                .metaDataHandler(Mocker.logging(MetaMethod.class, "M ", sw))
                .build(Mocker.logging(DataMethod.class, "D ", sw));

        for (int i = 0; i < 1; i++) {
            reader.readOne();
        }
        assertEquals("M meta[one]\n"
                , sw.toString().replace("\r", ""));

    }

    interface MetaMethod {
        void meta(String text);
    }

    interface DataMethod {
        void data(String text);
    }
}
