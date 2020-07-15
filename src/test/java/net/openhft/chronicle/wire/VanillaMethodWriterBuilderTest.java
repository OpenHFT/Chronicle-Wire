package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.bytes.MethodId;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.Mocker;
import net.openhft.chronicle.core.annotation.UsedViaReflection;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class VanillaMethodWriterBuilderTest extends WireTestCommon {

    @UsedViaReflection
    private final String name;
    private final boolean explicitContext;

    public VanillaMethodWriterBuilderTest(String name, boolean explicitContext) {
        this.name = name;
        this.explicitContext = explicitContext;
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> schemas() {
        List<Object[]> tests = new ArrayList<>();
        tests.add(new Object[]{"implied context", false});
        tests.add(new Object[]{"explicit context", true});
        return tests;
    }

    private static void update(String name1, Object s) {
        if (!(s instanceof MyDto))
            return;
        MyDto dto1 = (MyDto) s;
        assertEquals("some text", dto1.message);
        dto1.message = "hello world";
    }

    private static void check(MyDto dto) {
        assertEquals("hello world", dto.message);
    }

    public static class MyDto extends SelfDescribingMarshallable {
        String message;

        MyDto(final String message) {
            this.message = message;
        }
    }

    public interface MyEvent {
        void print(MyDto text);
    }

    @Test
    public void testUpdateInterceptor() {
        final Bytes<ByteBuffer> t = Bytes.elasticByteBuffer();
        try {
            Wire w = WireType.BINARY.apply(t);
            w.methodWriterBuilder(MyEvent.class)
                    .updateInterceptor(VanillaMethodWriterBuilderTest::update)
                    .build()
                    .print(new MyDto("some text"));
            w.methodReader((MyEvent) VanillaMethodWriterBuilderTest::check)
                    .readOne();
        } finally {
            t.releaseLast();
        }
    }

    @Test
    public void useMethodId_False() {
        assertEquals("" +
                        "0f 00 00 00                                     # msg-length\n" +
                        "b9 07 6d 65 74 68 6f 64 31                      # method1\n" +
                        "e5 68 65 6c 6c 6f                               # hello\n" +
                        "24 00 00 00                                     # msg-length\n" +
                        "b9 07 6d 65 74 68 6f 64 32                      # method2\n" +
                        "82 16 00 00 00                                  # MWB\n" +
                        "   05 77 6f 72 6c 64                               # hello\n" +
                        "   7b 00 00 00 00 00 00 00                         # value\n" +
                        "   d9 ce f7 53 e3 a5 0b 40                         # money\n" +
                        "12 00 00 00                                     # msg-length\n" +
                        "b9 07 6d 65 74 68 6f 64 33                      # method3\n" +
                        "a7 d2 02 96 49 00 00 00 00                      # 1234567890\n" +
                        "2b 00 00 00                                     # msg-length\n" +
                        "b9 07 6d 65 74 68 6f 64 34                      # method4\n" +
                        "82 1d 00 00 00                                  # MWB2\n" +
                        "   c5 68 65 6c 6c 6f e5 77 6f 72 6c 64             # hello\n" +
                        "   c5 76 61 6c 75 65 7b                            # value\n" +
                        "   c5 6d 6f 6e 65 79 94 80 8e 02                   # money\n",
                doUseMethodId(false));
    }

    @Test
    public void useMethodId() {
        assertEquals("" +
                        "08 00 00 00                                     # msg-length\n" +
                        "ba 01                                           # method1\n" +
                        "e5 68 65 6c 6c 6f                               # hello\n" +
                        "1d 00 00 00                                     # msg-length\n" +
                        "ba 02                                           # method2\n" +
                        "82 16 00 00 00                                  # MWB\n" +
                        "   05 77 6f 72 6c 64                               # hello\n" +
                        "   7b 00 00 00 00 00 00 00                         # value\n" +
                        "   d9 ce f7 53 e3 a5 0b 40                         # money\n" +
                        "0b 00 00 00                                     # msg-length\n" +
                        "ba 03                                           # method3\n" +
                        "a7 d2 02 96 49 00 00 00 00                      # 1234567890\n" +
                        "24 00 00 00                                     # msg-length\n" +
                        "ba 04                                           # method4\n" +
                        "82 1d 00 00 00                                  # MWB2\n" +
                        "   c5 68 65 6c 6c 6f e5 77 6f 72 6c 64             # hello\n" +
                        "   c5 76 61 6c 75 65 7b                            # value\n" +
                        "   c5 6d 6f 6e 65 79 94 80 8e 02                   # money\n",
                doUseMethodId(true));
    }

    @NotNull
    private String doUseMethodId(boolean useMethodIds) {
        HexDumpBytes bytes = new HexDumpBytes();
        BinaryWire wire = new BinaryWire(bytes);
        WithMethodId id = wire.methodWriterBuilder(WithMethodId.class).useMethodIds(useMethodIds).get();
        try (DocumentContext dc = explicitContext ? id.writingDocument() : null) {
            id.method1("hello");
        }
        try (DocumentContext dc = explicitContext ? id.writingDocument() : null) {
            id.method2(new MWB("world", 123, 3.456));
        }
        try (DocumentContext dc = explicitContext ? id.writingDocument() : null) {
            id.method3(1234567890L);
        }
        try (DocumentContext dc = explicitContext ? id.writingDocument() : null) {
            id.method4(new MWB2("world", 123, 3.456));
        }
        String s = bytes.toHexString();
        StringWriter sw = new StringWriter();
        MethodReader reader = wire.methodReader(Mocker.logging(WithMethodId.class, "", sw));
        for (int i = 0; i < 4; i++)
            assertTrue(reader.readOne());
        assertFalse(reader.readOne());
        assertEquals("method1[hello]\n" +
                "method2[!net.openhft.chronicle.wire.VanillaMethodWriterBuilderTest$MWB {\n" +
                "  hello: world,\n" +
                "  value: 123,\n" +
                "  money: 3.456\n" +
                "}\n" +
                "]\n" +
                "method3[1234567890]\n" +
                "method4[!net.openhft.chronicle.wire.VanillaMethodWriterBuilderTest$MWB2 {\n" +
                "  hello: world,\n" +
                "  value: 123,\n" +
                "  money: 3.456\n" +
                "}\n" +
                "]\n", sw.toString().replace("\r\n", "\n"));
        bytes.releaseLast();
        return s;
    }

    interface WithMethodId extends MethodWriterWithContext {
        @MethodId(1)
        void method1(String hello);

        @MethodId(2)
        void method2(MWB mwb);

        @MethodId(3)
        void method3(long value);

        @MethodId(4)
        void method4(MWB2 mwb);
    }

    static class MWB extends BytesInBinaryMarshallable {
        String hello;
        long value;
        double money;

        public MWB(String hello, long value, double money) {
            this.hello = hello;
            this.value = value;
            this.money = money;
        }

        @Override
        public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
            super.readMarshallable(wire);
        }
    }

    static class MWB2 extends SelfDescribingMarshallable {
        String hello;
        long value;
        double money;

        public MWB2(String hello, long value, double money) {
            this.hello = hello;
            this.value = value;
            this.money = money;
        }
    }

}