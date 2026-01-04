/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.util.ClassNotFoundRuntimeException;
import net.openhft.chronicle.core.util.Mocker;
import net.openhft.chronicle.wire.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class VanillaMethodReaderTest extends WireTestCommon {

    private A instance;

    @NotNull
    private static String asString(StringWriter out) {
        return out.toString().replace("\r", "");
    }

    @Test
    @DisplayName("Method reader handles metadata and data calls")
    public void testMethodReaderWriterMetadata() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for metadata reader test");

        Bytes<?> b = Bytes.allocateElasticOnHeap();
        try {
            Wire wire = WireType.BINARY.apply(b);
            wire.usePadding(true);

            {
                AListener aListener = wire.methodWriterBuilder(true, AListener.class).build();
                A a = new A();
                a.x = 5;
                aListener.a(a);
                // pretend to be system metadata
                aListener.index2index(a);
            }

            final AListener aListener = new AListener() {
                @Override
                public void a(final A a) {
                    VanillaMethodReaderTest.this.instance = a;
                }

                @Override
                public void index2index(Marshallable a) {
                    fail("metadata index2index should be ignored by reader");
                }
            };
            MethodReader methodReader = wire.methodReaderBuilder()
                    .metaDataHandler(Mocker.ignored(IgnoredMetaData.class), aListener)
                    .build(aListener);
            checkReaderType(methodReader);
            {
                boolean succeeded = methodReader.readOne();
                assertEquals(5, this.instance.x, "method parameter should be correctly deserialized from wire");
                assertTrue(succeeded, "method reader should successfully read data method call");
            }
            {
                boolean succeeded = methodReader.readOne();
                assertEquals(5, this.instance.x, "method parameter should be preserved across multiple reads");
                assertTrue(succeeded, "method reader should successfully read metadata method call");
            }
        } finally {
            b.releaseLast();
        }
    }

    @Test
    @DisplayName("Read expected method calls from text wire documents")
    public void readMethods() throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for readMethods test");

        Wire wire = new TextWire(BytesUtil.readFile("methods/in.yaml"))
                .useTextDocuments();
        Wire wire2 = new TextWire(Bytes.allocateElasticOnHeap())
                .useTextDocuments();
        MockMethods writer = wire2.methodWriter(MockMethods.class);
        MethodReader reader = wire.methodReader(writer);
        checkReaderType(reader);
        for (int i = 0; i < 3; i++) {
            assertTrue(reader.readOne(), "method reader should read method call " + (i + 1) + " from wire");
        }
        assertFalse(reader.readOne(), "method reader should return false when no more method calls exist in wire");
        // expected
        Bytes<?> expected = BytesUtil.readFile("methods/out.yaml");
        assertEquals(expected.toString().trim().replace("\r", ""), wire2.toString().trim(), "method calls read from wire should be re-written identically to expected output");
    }

    @Test
    @SuppressWarnings("unused")
    @DisplayName("Read collection method calls from wire documents")
    public void readMethodsCollections() throws IOException, InterruptedException {
        Wire wire = new TextWire(BytesUtil.readFile("methods-collections-in.yaml"))
                .useTextDocuments();
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(10);
        MockMethods mocker = Mocker.queuing(MockMethods.class, "", queue);
        MethodReader reader = wire.methodReader(mocker);
        checkReaderType(reader);
        for (int i = 0; i < 2; i++) {
            assertTrue(reader.readOne(), "method reader should read method call " + (i + 1) + " containing collections from wire");
        }
        assertFalse(reader.readOne(), "method reader should return false when all collection method calls have been read");
        assertEquals(2, queue.size(), "exactly two method invocations should be queued");
        queue.take();
        assertEquals("method3[[{field1=gidday, field2=1}, {field1=mate, field2=2}]]", queue.take(), "collection parameters should be correctly marshalled through wire format");
    }

    @Test
    @DisplayName("Reader dispatches subclass parameters correctly in order")
    public void testSubclasses() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap())
                .useTextDocuments();
        MRTListener writer = wire.methodWriter(MRTListener.class);
        writer.timed(1234567890_000_000L);
        MRT1 top1 = new MRT1("one");
        assertEquals("one", top1.field1, "MRT1 field1 should match constructor input for top1");
        assertEquals("a", top1.value, "MRT1 value should default to a");
        writer.top(top1);
        writer.method2("one", new MRT1("one"));
        MRT2 top2 = new MRT2("one", "two");
        assertEquals("one", top2.field1, "MRT2 field1 should match constructor input for top2");
        assertEquals("a", top2.value, "MRT2 value should default to a");
        assertEquals("two", top2.field2, "MRT2 field2 should match constructor input for top2");
        writer.top(top2);
        MRT1 mid1 = new MRT1("1");
        assertEquals("1", mid1.field1, "MRT1 field1 should match constructor input for mid1");
        writer.mid(mid1);
        writer.method2("1", new MRT1("1"));
        MRT2 mid2 = new MRT2("1", "2");
        assertEquals("1", mid2.field1, "MRT2 field1 should match constructor input for mid2");
        assertEquals("2", mid2.field2, "MRT2 field2 should match constructor input for mid2");
        writer.mid(mid2);

        StringWriter sw = new StringWriter();
        MethodReader reader = wire.methodReader(Mocker.logging(MRTListener.class, "subs ", sw));
        checkReaderType(reader);
        for (int i = 0; i < 7; i++) {
            assertTrue(reader.readOne(), "method reader should read method call " + (i + 1) + " with subclass parameter from wire");
        }
        assertFalse(reader.readOne(), "method reader should return false when all subclass method calls have been read");
        String expected = "subs timed[1234567890000000]\n" +
                "subs top[!net.openhft.chronicle.wire.method.VanillaMethodReaderTest$MRT1 {\n" +
                "  field1: one,\n" +
                "  value: a\n" +
                "}\n" +
                "]\n" +
                "subs method2[one, !net.openhft.chronicle.wire.method.VanillaMethodReaderTest$MRT1 {\n" +
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
                "subs method2[1, !net.openhft.chronicle.wire.method.VanillaMethodReaderTest$MRT1 {\n" +
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
        String actual = asString(sw);
        assertEquals(expected, actual, "method reader should correctly dispatch calls with polymorphic parameters to implementation");
    }

    @Test
    @DisplayName("Writer uses type metadata when interceptor is null")
    public void methodInterceptorNull() {
        Wire wire = new TextWire(Bytes.allocateElasticOnHeap())
                .useTextDocuments();
        MRTListener writer = wire.methodWriterBuilder(MRTListener.class)
                .build();
        writer.top(new MRT1("one"));
        writer.top(new MRT2("one", "two"));
        writer.mid(new MRT1("1"));
        writer.mid(new MRT2("1", "2"));

        assertEquals("top: !net.openhft.chronicle.wire.method.VanillaMethodReaderTest$MRT1 {\n" +
                "  field1: one,\n" +
                "  value: a\n" +
                "}\n" +
                "...\n" +
                "top: !net.openhft.chronicle.wire.method.VanillaMethodReaderTest$MRT2 {\n" +
                "  field1: one,\n" +
                "  value: a,\n" +
                "  field2: two\n" +
                "}\n" +
                "...\n" +
                "mid: {\n" +
                "  field1: \"1\",\n" +
                "  value: a\n" +
                "}\n" +
                "...\n" +
                "mid: !net.openhft.chronicle.wire.method.VanillaMethodReaderTest$MRT2 {\n" +
                "  field1: \"1\",\n" +
                "  value: a,\n" +
                "  field2: \"2\"\n" +
                "}\n" +
                "...\n", wire.toString(), "method writer should serialize polymorphic method calls with correct type information");
    }

    @Test
    @DisplayName("Nested unknown class is preserved in tuples")
    public void testNestedUnknownClass() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for nested unknown class test");

        Wire wire2 = new TextWire(Bytes.allocateElasticOnHeap())
                .useTextDocuments()
                .generateTuples(true);
        MRTListener writer2 = wire2.methodWriter(MRTListener.class);

        String text = "unknown: {\n" +
                "  u: !UnknownClass2 {\n" +
                "    one: 1,\n" +
                "    two: 2.2,\n" +
                "    three: words\n" +
                "  }\n" +
                "}\n" +
                "...\n";
        Wire wire = TextWire.from(text)
                .useTextDocuments()
                .generateTuples(true);
        MethodReader reader = wire.methodReader(writer2);
        checkReaderType(reader);
        assertTrue(reader.readOne(), "method reader should successfully read method call with nested unknown class");
        assertFalse(reader.readOne(), "method reader should return false after reading all available method calls");
        assertEquals(text, wire2.toString(), "nested unknown class should be preserved through wire round-trip with tuple generation");
        NestedUnknown unknown = new NestedUnknown();
        assertNull(unknown.u, "NestedUnknown u should default to null");
    }

    @Test
    @DisplayName("Unknown class does not throw with tuple generation enabled")
    public void testUnknownClassDoesntThrow() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for unknown class tuple test");

        Wire wire2 = new TextWire(Bytes.allocateElasticOnHeap())
                .useTextDocuments()
                .generateTuples(true);
        MRTListener writer2 = wire2.methodWriter(MRTListener.class);

        String text = "top: !UnknownClass {\n" +
                "  one: 1,\n" +
                "  two: 2.2,\n" +
                "  three: words\n" +
                "}\n" +
                "...\n" +
                "top: {\n" +
                "  one: 11,\n" +
                "  two: 22.2,\n" +
                "  three: many words\n" +
                "}\n" +
                "...\n";
        Wire wire = TextWire.from(text)
                .useTextDocuments()
                .generateTuples(true);
        MethodReader reader = wire.methodReader(writer2);
        checkReaderType(reader);
        assertTrue(reader.readOne(), "method reader should read first method call with unknown type without throwing");
        assertTrue(reader.readOne(), "method reader should read second method call with known type without throwing");
        assertFalse(reader.readOne(), "method reader should return false after reading all method calls with unknown types");
        assertEquals(text, wire2.toString(), "unknown class types should be preserved through wire round-trip when tuple generation is enabled");
    }

    @Test
    @DisplayName("Unknown class throws without tuple generation")
    public void testUnknownClassThrow() {
        assertThrows(ClassNotFoundRuntimeException.class, () -> {
            assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for unknown class throw test");

            Wire wire2 = new TextWire(Bytes.allocateElasticOnHeap())
                    .useTextDocuments()
                    .generateTuples(false);
            MRTListener writer2 = wire2.methodWriter(MRTListener.class);

            String text = "top: !UnknownClass {\n" +
                    "  one: 1,\n" +
                    "  two: 2.2,\n" +
                    "  three: words\n" +
                    "}\n" +
                    "...\n" +
                    "top: {\n" +
                    "  one: 11,\n" +
                    "  two: 22.2,\n" +
                    "  three: many words\n" +
                    "}\n" +
                    "...\n";
            Wire wire = TextWire.from(text)
                    .useTextDocuments()
                    .generateTuples(false);
            MethodReader reader = wire.methodReader(writer2);
            checkReaderType(reader);
            assertTrue(reader.readOne(), "method reader should attempt to read first method call with unknown type");
            assertTrue(reader.readOne(), "method reader should attempt to read second method call");
            assertFalse(reader.readOne(), "method reader should return false after attempting all reads");
            assertEquals(text, wire2.toString(), "wire content should match expected format");
        }, "Unknown class should fail without tuple generation");
    }

    @Test
    @DisplayName("Message history clears after method reader")
    public void testMessageHistoryCleared() {
        Assumptions.assumeFalse(Boolean.getBoolean("history.as.bytes"),
                "History-as-bytes mode changes message history handling");
        try {
            Wire wire = new TextWire(Bytes.allocateElasticOnHeap()).useTextDocuments();
            final long sourceIndex = 2L;
            MessageHistory.get().reset(1, sourceIndex);

            wire.write(MethodReader.HISTORY).marshallable(MessageHistory.get());
            MRTListener writer = wire.methodWriter(MRTListener.class);
            writer.timed(1234L);

            MethodReader reader = wire.methodReader(Mocker.intercepting(MRTListener.class, (s, objects) -> {
                assertEquals("timed", s, "intercepted method name should match the method being invoked");
                assertEquals(1, MessageHistory.get().sources(), "message history should contain exactly one source during method dispatch");
                assertEquals(sourceIndex, MessageHistory.get().sourceIndex(0), "message history should preserve the original source index");
            }, null));
            checkReaderType(reader);
            assertTrue(reader.readOne(), "method reader should successfully read method call with message history");
            assertFalse(reader.readOne(), "method reader should return false after reading all method calls");
            assertEquals(0, MessageHistory.get().sources(), "message history should be cleared after method reader completes processing");
        } finally {
            MessageHistory.clear();
        }
    }

    @Test
    @DisplayName("Overloaded method names should be rejected by reader")
    public void testOverloaded() {
        assertThrows(IllegalStateException.class, () -> {
            Jvm.recordExceptions();
            try {
                Wire wire2 = WireType.TEXT.apply(Bytes.allocateElasticOnHeap(32));
                Overloaded writer2 = wire2.methodWriter(Overloaded.class);
                Wire wire = TextWire.from("method: [ ]\n");
                wire.methodReader(writer2);
            } finally {
                Jvm.resetExceptionHandlers();
            }
        }, "Overloaded method definitions should fail reader creation");
    }

    @Test
    @DisplayName("Parse metadata without scanning mode enabled")
    public void parseMetaData() {
        assertTrue(doParseMetaData(false), "Metadata parse should succeed without scanning");
    }

    @Test
    @DisplayName("Parse metadata with scanning mode enabled")
    public void parseMetaDataScanning() {
        assertTrue(doParseMetaData(true), "Metadata parse should succeed with scanning enabled");
    }

    private boolean doParseMetaData(boolean scanning) {
        Wire wire = WireType.BINARY_LIGHT.apply(new HexDumpBytes());
        try {
            final RoutedSaying routedSaying = wire.methodWriter(RoutedSaying.class);
            final RoutedSaying metaRoutedSaying = wire.methodWriterBuilder(true, RoutedSaying.class).build();
            metaRoutedSaying.to("aye").say("hi AAA");
            routedSaying.to("one").say("hi 111");
            metaRoutedSaying.to("bee").say("hi BBB");
            routedSaying.to("two").say("hi 222");
            assertEquals("14 00 00 40                                     # msg-length\n" +
                            "b9 02 74 6f                                     # to: (event)\n" +
                            "e3 61 79 65                                     # aye\n" +
                            "b9 03 73 61 79                                  # say: (event)\n" +
                            "e6 68 69 20 41 41 41                            # hi AAA\n" +
                            "14 00 00 00                                     # msg-length\n" +
                            "b9 02 74 6f                                     # to: (event)\n" +
                            "e3 6f 6e 65                                     # one\n" +
                            "b9 03 73 61 79                                  # say: (event)\n" +
                            "e6 68 69 20 31 31 31                            # hi 111\n" +
                            "14 00 00 40                                     # msg-length\n" +
                            "b9 02 74 6f                                     # to: (event)\n" +
                            "e3 62 65 65                                     # bee\n" +
                            "b9 03 73 61 79                                  # say: (event)\n" +
                            "e6 68 69 20 42 42 42                            # hi BBB\n" +
                            "14 00 00 00                                     # msg-length\n" +
                            "b9 02 74 6f                                     # to: (event)\n" +
                            "e3 74 77 6f                                     # two\n" +
                            "b9 03 73 61 79                                  # say: (event)\n" +
                            "e6 68 69 20 32 32 32                            # hi 222\n",
                    wire.bytes().toHexString(), "method writer should serialize metadata and data messages with correct binary format");
            StringWriter out = new StringWriter();
            final MethodReader reader = wire.methodReaderBuilder()
                    .scanning(scanning)
                    .metaDataHandler(Mocker.logging(RoutedSaying.class, "meta: ", out))
                    .build(Mocker.logging(RoutedSaying.class, "data: ", out));

            if (!scanning) {
                assertTrue(reader.readOne(), "method reader should read first metadata message when not in scanning mode");
                assertEquals("meta: to[aye]\n" +
                                "meta: say[hi AAA]\n",
                        asString(out), "first metadata method calls should be dispatched to metadata handler");
            }

            assertTrue(reader.readOne(), "method reader should read first data message");
            assertEquals("meta: to[aye]\n" +
                            "meta: say[hi AAA]\n" +
                            "data: to[one]\n" +
                            "data: say[hi 111]\n",

                    asString(out), "metadata and data method calls should be dispatched to appropriate handlers");
            if (!scanning) {
                assertTrue(reader.readOne(), "method reader should read second metadata message when not in scanning mode");
                assertEquals("meta: to[aye]\n" +
                                "meta: say[hi AAA]\n" +
                                "data: to[one]\n" +
                                "data: say[hi 111]\n" +
                                "meta: to[bee]\n" +
                                "meta: say[hi BBB]\n",
                        asString(out), "second metadata method calls should be appended to output");
            }

            assertTrue(reader.readOne(), "method reader should read second data message");
            assertEquals("meta: to[aye]\n" +
                            "meta: say[hi AAA]\n" +
                            "data: to[one]\n" +
                            "data: say[hi 111]\n" +
                            "meta: to[bee]\n" +
                            "meta: say[hi BBB]\n" +
                            "data: to[two]\n" +
                            "data: say[hi 222]\n",
                    asString(out), "all metadata and data method calls should be correctly ordered in output");

            assertFalse(reader.readOne(), "method reader should return false when all messages have been read");
            return true;
        } finally {
            wire.bytes().releaseLast();
        }
    }

    private void checkReaderType(MethodReader reader) {
        assertFalse(Proxy.isProxyClass(reader.getClass()), "method reader should be a generated class, not a dynamic proxy");
    }

    interface IgnoredMetaData {
        void header(Marshallable marshallable);

        void index(Marshallable marshallable);

        void index2index(Marshallable marshallable);

        void roll(Marshallable marshallable);
    }

    interface Saying {
        void say(String say);
    }

    interface Routed<T> {
        T to(String target);
    }

    interface RoutedSaying extends Routed<Saying> {

    }

    // keep package local.
    interface AListener {
        void a(A a);

        // this pretends to be system metadata
        void index2index(Marshallable a);
    }

    interface MRTInterface {

    }

    interface MRTListener {
        void timed(long time);

        void top(MRTInterface mrti);

        void mid(MRT1 mrt1);

        void method2(String key, MRT1 mrt);

        void unknown(NestedUnknown unknown);
    }

    interface Overloaded {
        void method();

        void method(MockDto dto);
    }

    static class A extends SelfDescribingMarshallable {
        int x;
    }

    @SuppressFBWarnings(
            value = {"URF_UNREAD_FIELD", "UWF_UNWRITTEN_FIELD", "UUF_UNUSED_FIELD"},
            justification = "Fields are populated via Wire marshalling in tests.")
    static class NestedUnknown extends SelfDescribingMarshallable {
        Marshallable u;
    }

    static class MRT1 extends SelfDescribingMarshallable implements MRTInterface {
        final String field1;
        String value = "a";

        MRT1(String field1) {
            this.field1 = field1;
        }
    }

    static class MRT2 extends MRT1 {
        final String field2;

        MRT2(String field1, String field2) {
            super(field1);
            this.field2 = field2;
        }
    }
}
