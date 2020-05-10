package net.openhft.chronicle.wire;

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
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class VanillaMethodWriterBuilderTest {

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
        String s = null;
        try {
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

            // "[pos: 0, rlim: 96, wlim: 8EiB, cap: 8EiB ] ǁ⒏٠٠٠º⒈åhello\\u001D٠٠٠º⒉\\u0082\\u0016٠٠٠⒌world{٠٠٠٠٠٠٠ÙÎ÷Sã¥⒒@⒎٠٠٠º⒊¦Ò⒉\\u0096I$٠٠٠º⒋\\u0082\\u001D٠٠٠ÅhelloåworldÅvalue{Åmoney\\u0094\\u0080\\u008E⒉٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠⒛٠⒈٠T>6⒌realUnderlyingObject\\u0081\u007F٠٠"
            // "[pos: 0, rlim: 100, wlim: 8EiB, cap: 8EiB] ǁ⒏٠٠٠º⒈åhello \u001D٠٠٠º⒉\u0082  \u0016٠٠٠⒌world{٠٠٠٠٠٠٠ÙÎ÷Sã¥⒒@⒒٠٠٠º⒊§Ò⒉  \u0096I٠٠٠٠$٠٠٠º⒋\u0082\u001D٠٠٠ÅhelloåworldÅvalue{Åmoney\u0094\u0080\u008E⒉٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠0ú⒏F⒎٠٠٠À1ôÿè٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠٠@6ôÿ
            System.out.println(bytes.toDebugString());
            s = bytes.toHexString();
            StringWriter sw = new StringWriter();

            /**
             * 00 00 00 80                                     # msg-length
             * ba 01                                           # method1
             * e5 68 65 6c 6c 6f                               # hello
             * 00 00 00 80                                     # msg-length
             * ba 02                                           # method2
             * 82 16 00 00 00                                  # MWB
             *    05 77 6f 72 6c 64                               # hello
             *    7b 00 00 00 00 00 00 00                         # value
             *    d9 ce f7 53 e3 a5 0b 40                         # money
             * 00 00 00 80                                     # msg-length
             * ba 03                                           # method3
             * a7 d2 02 96 49 00 00 00 00                      # 1234567890
             * 00 00 00 80                                     # msg-length
             * ba 04                                           # method4
             * 82 1d 00 00 00                                  # MWB2
             *    c5 68 65 6c 6c 6f e5 77 6f 72 6c 64             # hello
             *    c5 76 61 6c 75 65 7b                            # value
             *    c5 6d 6f 6e 65 79 94 80 8e 02                   # money
             */

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
        } finally {
            bytes.release();
        }

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