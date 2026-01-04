/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.core.util.Mocker;
import net.openhft.chronicle.wire.*;
import org.easymock.EasyMock;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;
import static org.easymock.EasyMock.*;

public class MethodWriterTest extends WireTestCommon {
    @Test
    @DisplayName("Method writer serialises subclass events correctly")
    void testSubclasses() {
        Wire wire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap(256));

        Event writer = wire.methodWriterBuilder(Event.class).genericEvent("event").build();
        writer.event("top", new VanillaMethodReaderTest.MRT1("one"));
        writer.event("top", new VanillaMethodReaderTest.MRT2("one", "two"));
        writer.event("mid", new VanillaMethodReaderTest.MRT1("1"));
        writer.event("mid", new VanillaMethodReaderTest.MRT2("1", "2"));

        /*
         * top: !net.openhft.chronicle.wire.method.VanillaMethodReaderTest$MRT1 {
         *   field1: one,
         *   value: a
         * }
         * ---
         * top: !net.openhft.chronicle.wire.method.VanillaMethodReaderTest$MRT2 {
         *   field1: one,
         *   value: a,
         *   field2: two
         * }
         * ---
         * mid: !net.openhft.chronicle.wire.method.VanillaMethodReaderTest$MRT1 {
         *   field1: "1",
         *   value: a
         * }
         * ---
         * mid: !net.openhft.chronicle.wire.method.VanillaMethodReaderTest$MRT2 {
         *   field1: "1",
         *   value: a,
         *   field2: "2"
         * }
         * ---
         */
        StringWriter sw = new StringWriter();
        MethodReader reader = wire.methodReader(Mocker.logging(VanillaMethodReaderTest.MRTListener.class, "subs ", sw));
        for (int i = 0; i < 4; i++) {
            assertTrue(reader.readOne(), "method reader should read event call " + (i + 1) + " of 4");
        }

        assertFalse(reader.readOne(), "method reader should have no more events after reading all 4");
        String expected = "subs top[!net.openhft.chronicle.wire.method.VanillaMethodReaderTest$MRT1 {\n" +
                "  field1: one,\n" +
                "  value: a\n" +
                "}\n" +
                "]\n" +
                "subs top[!net.openhft.chronicle.wire.method.VanillaMethodReaderTest$MRT2 {\n" +
                "  field1: one,\n" +
                "  value: a,\n" +
                "  field2: two\n" +
                "}\n" +
                "]\n" +
                "subs mid[!net.openhft.chronicle.wire.method.VanillaMethodReaderTest$MRT1 {\n" +
                "  field1: \"1\",\n" +
                "  value: a\n" +
                "}\n" +
                "]\n" +
                "subs mid[!net.openhft.chronicle.wire.method.VanillaMethodReaderTest$MRT2 {\n" +
                "  field1: \"1\",\n" +
                "  value: a,\n" +
                "  field2: \"2\"\n" +
                "}\n" +
                "]\n";
        String actual = sw.toString().replace("\r", "");
        assertEquals(expected, actual, "Logged output should match expected subclass events");
    }

    @Test
    @DisplayName("Default method calls are written to wire")
    void testDefault() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap(256))
                .useTextDocuments();

        HasDefault writer = wire.methodWriter(HasDefault.class);
        checkWriterType(writer);
        writer.callToDefaultMethod("hello world");

        assertTrue(wire.toString().startsWith("callToDefaultMethod: hello world"),
                "Wire output should start with default method call");
    }

    @Test
    @SuppressWarnings("deprecation")
    @DisplayName("Method writer can write to multiple outputs")
    void multiOut() {
        Wire wire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap());

        Event event = wire.methodWriter(Event.class);
        checkWriterType(event);
        event.event("one", "one");
        BinaryWire wire2 = new BinaryWire(new HexDumpBytes());
        wire2.usePadding(true);

        ((MethodWriter) event).marshallableOut(wire2);
        event.event("two", "two");
        assertEquals("event: [\n" +
                "  one,\n" +
                "  one\n" +
                "]\n" +
                "...\n", wire.toString(),
                "Text wire should contain the first event only");
        assertEquals("14 00 00 00                                     # msg-length\n" +
                        "b9 05 65 76 65 6e 74                            # event: (event)\n" +
                        "82 08 00 00 00                                  # sequence\n" +
                        "e3 74 77 6f                                     # two\n" +
                        "e3 74 77 6f                                     # two\n",
                wire2.bytes().toHexString(),
                "Binary output should contain the second event");
        wire2.bytes().releaseLast();
    }

    @Test
    @DisplayName("Static Closeable methods are ignored by writer")
    void ignoreStatic() {
        Wire wire = WireType.TEXT.apply(Bytes.allocateElasticOnHeap(256));

        Closeable writer = wire.methodWriter(Closeable.class);
        checkWriterType(writer);
        Closeable.closeQuietly(writer);
        assertEquals("", wire.toString(), "Static Closeable methods should not emit output");
    }

    @Test
    @DisplayName("No-args methods serialise as empty strings")
    void testNoArgs() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap(256))
                .useTextDocuments();

        NoArgs writer = wire.methodWriter(NoArgs.class);
        checkWriterType(writer);
        writer.methodOne();
        writer.methodTwo();
        assertEquals("methodOne: \"\"\n" +
                "...\n" +
                "methodTwo: \"\"\n" +
                "...\n", wire.toString(),
                "No-args methods should serialize as empty strings");
        NoArgs mock = createMock(NoArgs.class);
        mock.methodOne();
        mock.methodTwo();
        EasyMock.replay(mock);
        MethodReader reader = wire.methodReader(mock);
        for (int i = 0; i < 3; i++)
            assertEquals(i < 2, reader.readOne(), "method reader should read exactly 2 method calls at index " + i);
        verify(mock);
    }

    @Test
    @DisplayName("Update interceptor captures method argument text")
    void testUpdateListener() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap(256))
                .useTextDocuments();

        final StringBuilder value = new StringBuilder();

        StringMethod instance = wire.methodWriterBuilder(StringMethod.class).updateInterceptor((methodName, t) -> {
            value.append(t);
            return true;
        }).build();
        checkWriterType(instance);

        String expected = "hello world";
        instance.method(expected);
        assertEquals(expected, value.toString(),
                "Update interceptor should capture the argument text");

        assertTrue(wire.toString().startsWith("method: hello world\n" +
                "...\n"), "Wire output should start with update-intercepted call");
    }

    @Test
    @DisplayName("Update interceptor suppresses writes when returning false")
    void testUpdateListenerCheckUpdateInterceptorReturnValue() {
        final Wire wire = new TextWire(Bytes.allocateElasticOnHeap(256)).useTextDocuments();

        StringMethod instance = wire.methodWriterBuilder(StringMethod.class).updateInterceptor((methodName, t) -> false).build();
        checkWriterType(instance);
        instance.method(" this should not be written because the return value above is false");

        assertEquals("", wire.toString(),
                "Update interceptor should suppress writes when returning false");
    }

    @Test
    @DisplayName("Micro timestamp arguments serialise with expected format")
    void testMicroTS() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap(256))
                .useTextDocuments();

        HasMicroTS writer = wire.methodWriter(HasMicroTS.class);
        checkWriterType(writer);
        long now = 1532251709775811L;
        MicroTS microTS = new MicroTS();
        microTS.timeUS = now;
        assertEquals(now, microTS.timeUS, "MicroTS timeUS should match assigned value");
        writer.microTS(microTS);
        assertEquals("microTS: {\n" +
                "  timeUS: 2018-07-22T09:28:29.775811\n" +
                "}\n" +
                "...\n", wire.toString(),
                "MicroTimestamp should serialize in expected format");
        HasMicroTS mock = createMock(HasMicroTS.class);
        MethodReader reader = wire.methodReader(mock);
        mock.microTS(microTS);
        replay(mock);
        for (int i = 0; i < 2; i++)
            assertEquals(i < 1, reader.readOne(), "MethodReader should read a single microTS call at index " + i);
        verify(mock);
    }

    @Test
    @DisplayName("Primitive arguments round-trip via method writer")
    void testPrimitives() {
        assertTrue(doTestPrimitives(false), "primitive arguments should survive method writer serialization round-trip");
    }

    boolean doTestPrimitives(boolean byteShort) {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap(256)).useTextDocuments();

        Args writer = wire.methodWriter(Args.class);
        checkWriterType(writer);
        writer.primitives(true, (byte) 1, (short) 2, 3, 4, '5', 6, 7, "8", "9");
        assertEquals("primitives: [\n" +
                "  true,\n" +
                "  1,\n" +
                "  2,\n" +
                "  3,\n" +
                "  4,\n" +
                "  \"5\",\n" +
                "  6.0,\n" +
                "  7.0,\n" +
                "  \"8\",\n" +
                "  \"9\"\n" +
                "]\n" +
                "...\n", wire.toString(),
                "Primitive arguments should serialize in order");
        Args mock = createMock(Args.class);
        mock.primitives(true, (byte) 1, (short) 2, 3, 4, '5', 6, 7, "8", "9");
        EasyMock.replay(mock);
        MethodReader reader = wire.methodReader(mock);
        for (int i = 0; i < 2; i++)
            assertEquals(i < 1, reader.readOne(), "method reader should read exactly 1 primitives call at index " + i);
        verify(mock);
        return true;
    }

    @Test
    @DisplayName("Marshalling exceptions roll back partial writes")
    void testExceptionInMarshallingRollsBack() {
        final Wire wire = new TextWire(Bytes.allocateElasticOnHeap(256)).useTextDocuments();

        HasMarshallable instance = wire.methodWriterBuilder(HasMarshallable.class).build();
        checkWriterType(instance);
        try {
            instance.method(AMarshallable.EXCEPTION);
        } catch (NullPointerException npe) {
            // ignore
        }
        assertEquals("", wire.toString(), "half message should not be written");
    }

    @Test
    @DisplayName("Multiple interface inheritance resolves ignore method")
    void testMultipleImplsInheritBoth() {
        final Wire wire = new TextWire(Bytes.allocateElasticOnHeap(256)).useTextDocuments();

        InheritBoth instance = wire.methodWriterBuilder(InheritBoth.class).build();
        checkWriterType(instance);
    }

    @Test
    @Disabled("https://github.com/OpenHFT/Chronicle-Wire/issues/274")
    @DisplayName("Multiple interface return values are unsupported")
    void testMultipleImplsReturnValues() {
        final Wire wire = new TextWire(Bytes.allocateElasticOnHeap(256)).useTextDocuments();

        ReturnValues instance = wire.methodWriterBuilder(ReturnValues.class).build();
        checkWriterType(instance);
    }

    @Test
    @DisplayName("Return value workaround uses inherited interfaces")
    void testMultipleImplsReturnValuesWorkAround() {
        final Wire wire = new TextWire(Bytes.allocateElasticOnHeap(256)).useTextDocuments();

        ReturnValuesWorkAround instance = wire.methodWriterBuilder(ReturnValuesWorkAround.class).build();
        checkWriterType(instance);
    }

    void checkWriterType(Object writer) {
        assertFalse(Proxy.isProxyClass(writer.getClass()),
                "Writer should be generated rather than a proxy");
    }

    @FunctionalInterface
    public interface Event {
        void event(String eventName, Object o);
    }

    @FunctionalInterface
    public interface HasDefault {
        default void callToDefaultMethod(String value) {

        }

        void method(String args);
    }

    public interface StringMethod {
        void method(String value);
    }

    public interface NoArgs {
        void methodOne();

        void methodTwo();
    }

    public interface Args {
        void primitives(boolean n, byte b, short s, int i, long l, char c, float f, double d, String s1, CharSequence s2);
    }

    public interface HasMicroTS {
        void microTS(MicroTS microTS);
    }

    public static class MicroTS extends SelfDescribingMarshallable {
        @LongConversion(MicroTimestampLongConverter.class)
        long timeUS;
    }

    public static class AMarshallable extends SelfDescribingMarshallable {
        private static final AMarshallable EXCEPTION = new AMarshallable();

        @Override
        public void writeMarshallable(@NotNull WireOut wire) {
            if (this == EXCEPTION)
                throw new NullPointerException("writeMarshallable failed. Should now rollback");
        }
    }

    public interface HasMarshallable {
        void method(AMarshallable exception);
    }

    public interface IgnoreMethod1 {
        default boolean ignoreMethodBasedOnFirstArg(String methodName, String ladderDefinitionName) {
            return false;
        }
    }

    public interface IgnoreMethod2 {
        default boolean ignoreMethodBasedOnFirstArg(String methodName, String ladderDefinitionName) {
            return false;
        }
    }

    interface InheritBoth extends IgnoreMethod1, IgnoreMethod2 {
        @Override
        default boolean ignoreMethodBasedOnFirstArg(String methodName, String ladderDefinitionName) {
            return false;
        }
    }

    interface ReturnValues {
        IgnoreMethod1 ignoreMethod1();
        IgnoreMethod2 ignoreMethod2();
    }

    interface ReturnValuesWorkAround extends ReturnValues, IgnoreMethod1, IgnoreMethod2 {
        @Override
        default boolean ignoreMethodBasedOnFirstArg(String methodName, String ladderDefinitionName) {
            return false;
        }
    }
}
