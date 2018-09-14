package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.bytes.MethodId;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VanillaMethodWriterBuilderTest {

    @Test
    public void useMethodId_False() {
        assertEquals(
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
                        "   c5 6d 6f 6e 65 79 94 80 8e 02                   # money\n", doUseMethodId(false));
    }

    @Test
    public void useMethodId() {
        assertEquals(
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
                        "   c5 6d 6f 6e 65 79 94 80 8e 02                   # money\n", doUseMethodId(true));
    }

    @NotNull
    private String doUseMethodId(boolean useMethodIds) {
        HexDumpBytes bytes = new HexDumpBytes();
        BinaryWire wire = new BinaryWire(bytes);
        WithMethodId id = wire.methodWriterBuilder(WithMethodId.class).useMethodIds(useMethodIds).build();
        id.method1("hello");
        id.method2(new MWB("world", 123, 3.456));
        id.method3(1234567890L);
        id.method4(new MWB2("world", 123, 3.456));

        String s = bytes.toHexString();
        bytes.release();
        return s;
    }

    interface WithMethodId {
        @MethodId(1)
        void method1(String hello);

        @MethodId(2)
        void method2(MWB mwb);

        @MethodId(3)
        void method3(long value);

        @MethodId(4)
        void method4(MWB2 mwb);
    }

    static class MWB extends AbstractBytesMarshallable {
        String hello;
        long value;
        double money;

        public MWB(String hello, long value, double money) {
            this.hello = hello;
            this.value = value;
            this.money = money;
        }
    }

    static class MWB2 extends AbstractMarshallable {
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