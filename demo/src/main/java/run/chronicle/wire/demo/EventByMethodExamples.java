package run.chronicle.wire.demo;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.bytes.MethodId;
import net.openhft.chronicle.bytes.util.BinaryLengthLength;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.time.SystemTimeProvider;
import net.openhft.chronicle.wire.*;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class EventByMethodExamples {
    static {
        Jvm.pause(10);
    }

    public static void main(String[] args) {
        noArgs();
        primArg();
        withMethodId();
        twoPrimArg();
        scalarArg();
        timeAsLong();
        withDto();
        chainedMethod();
    }

    private static void noArgs() {
        dump(Examples.class,
                "No Arguments",
                "Any event type, single method calls only, with no arguments",
                "eg.noArgs()",
                Examples::noArgs);
    }

    private static void primArg() {
        dump(Examples.class,
                "Primitive argument",
                "An event type with a single primitive arguments",
                "eg.primArg(1.5)",
                eg -> eg.primArg(1.5));
    }

    private static void withMethodId() {
        dump(Examples.class,
                "Using an @MethodId Primitive argument",
                "An event type as a methodId with a single primitive arguments",
                "eg.withMethodId(150)",
                eg -> eg.withMethodId(150));
    }

    private static void twoPrimArg() {
        dump(Examples.class,
                "Two primitive argument",
                "An event type with a two primitive arguments",
                "eg.primArg('A', 128)",
                eg -> eg.twoPrimArg('A', 128));
    }

    private static void scalarArg() {
        dump(Examples.class,
                "One scalar primitive argument",
                "An event type with a scalar arguments",
                "eg.scalarArg(TimeUnit.DAYS)",
                eg -> eg.scalarArg(TimeUnit.DAYS));
    }

    private static void timeAsLong() {
        long now = SystemTimeProvider.CLOCK.currentTimeNanos();
        dump(Examples.class,
                "A timestamp as a long",
                "An event type with a local date time as a long arguments",
                "eg.timeNanos(NanoTimestampLongConverter.INSTANCE.parse(\""
                        + NanoTimestampLongConverter.INSTANCE.asString(now)
                        + "\"))",
                eg -> eg.timeNanos(now));
    }

    private static void withDto() {
        long now = SystemTimeProvider.CLOCK.currentTimeNanos();
        dump(Examples.class,
                "Event with a Data Transfer Object",
                "An event type with a flat DTO",
                "eg.withDto(new MyTypes().b((byte) -1).s((short) 1111).f(1.28f).i(66666).d(1.01).text(\"hello world\").ch('$').flag(true))",
                eg -> eg.withDto(new MyTypes().b((byte) -1).s((short) 1111).f(1.28f).i(66666).d(1.1234).text("hello world").ch('$').flag(true)));
    }

    private static void chainedMethod() {
        long now = SystemTimeProvider.CLOCK.currentTimeNanos();
        dump(DestinationTimedSaying.class,
                "Chained Event",
                "An event type can be chained together to compose routing or monitoring",
                "eg.via(\"target\").at(now).say(\"Hello World\")",
                eg -> eg.via("target").at(now).say("Hello World"));
    }

    static <T> void dump(Class<T> tClass, String methodName, String description, String codeStr, Consumer<T> code) {
        System.out.println("=== " + methodName);

        System.out.println();
        System.out.println(description);
        System.out.println();

        System.out.println("[source,java]");
        System.out.println("----");
        System.out.println(codeStr + ";");
        System.out.println("----");
        System.out.println();

        Wire yamlWire = new YamlWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
        code.accept(yamlWire.methodWriter(tClass));

        System.out.println("." + methodName + " As YAML ");
        System.out.println("[source,yaml]");
        System.out.println("----");
        System.out.print(yamlWire);
        System.out.println("----");
        System.out.println();

        Wire jsonWire = new JSONWire(Bytes.allocateElasticOnHeap()).useTextDocuments().trimFirstCurly(false);
        code.accept(jsonWire.methodWriter(tClass));

        System.out.println("." + methodName + " As JSON ");
        System.out.println("[source,json]");
        System.out.println("----");
        System.out.print(jsonWire);
        System.out.println("----");
        System.out.println();

        Wire binaryWire = WireType.BINARY_LIGHT.apply(new HexDumpBytes());
        code.accept(binaryWire.methodWriter(tClass));

        System.out.println("." + methodName + " As Binary YAML ");
        System.out.println("[source,text]");
        System.out.println("----");
        System.out.print(binaryWire.bytes().toHexString());
        System.out.println("----");
        System.out.println();
    }

    interface Examples {
        void noArgs();

        void primArg(double value);

        @MethodId(12)
        void withMethodId(long value);

        void twoPrimArg(char ch, long value);

        void scalarArg(TimeUnit timeUnit);

        void timeNanos(@LongConversion(NanoTimestampLongConverter.class) long timeNanos);

        void withDto(MyTypes dto);
    }

    interface Saying {
        void say(String hello);
    }

    interface Timed<T> {
        T at(@LongConversion(NanoTimestampLongConverter.class) long time);
    }

    interface TimedSaying extends Timed<Saying> {

    }

    interface Destination<T> {
        T via(String via);
    }

    interface DestinationTimedSaying extends Destination<TimedSaying> {

    }

    static class MyTypes extends SelfDescribingMarshallable {
        final StringBuilder text = new StringBuilder();
        boolean flag;
        byte b;
        short s;
        char ch;
        int i;
        float f;
        double d;
        long l;

        public MyTypes flag(boolean b) {
            this.flag = b;
            return this;
        }

        public boolean flag() {
            return this.flag;
        }

        public byte b() {
            return b;
        }

        public MyTypes b(byte b) {
            this.b = b;
            return this;
        }

        public MyTypes s(short s) {
            this.s = s;
            return this;
        }

        public short s() {
            return this.s;
        }

        public char ch() {
            return ch;
        }

        public MyTypes ch(char ch) {
            this.ch = ch;
            return this;
        }

        public float f() {
            return f;
        }

        public MyTypes f(float f) {
            this.f = f;
            return this;
        }

        public MyTypes d(double d) {
            this.d = d;
            return this;
        }

        public double d() {
            return this.d;
        }

        public MyTypes l(long l) {
            this.l = l;
            return this;
        }

        public long l() {
            return this.l;
        }

        public MyTypes i(int i) {
            this.i = i;
            return this;
        }

        public int i() {
            return this.i;
        }

        public StringBuilder text() {
            return text;
        }

        public MyTypes text(CharSequence value) {
            text.setLength(0);
            text.append(value);
            return this;
        }

        @Override
        public BinaryLengthLength binaryLengthLength() {
            return BinaryLengthLength.LENGTH_8BIT;
        }
    }
}
