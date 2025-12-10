/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.DocumentContext;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

import java.util.Arrays;
import java.security.InvalidAlgorithmParameterException;
import java.util.function.Supplier;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@SuppressWarnings("deprecation")
final class WireTestSupport {
    private WireTestSupport() {
    }

    static void assertWireTypeRoundTrip(Wire wire, String expectedText) {
        wire.write().object(WireType.BINARY)
                .write().object(WireType.TEXT)
                .write().object(WireType.RAW);

        assertEquals(expectedText, wire.toString());

        assertEquals(WireType.BINARY, wire.read().object(Object.class));
        assertEquals(WireType.TEXT, wire.read().object(Object.class));
        assertEquals(WireType.RAW, wire.read().object(Object.class));
    }

    static void assertLzwCompressionAsText(Wire wire, Supplier<Bytes<?>> bytesSupplier) {
        @NotNull final String s = "xxxxxxxxxxxxxxxxxxx2xxxxxxxxxxxxxxxxxxxxxxx";
        @NotNull String str = s + s + s + s;
        @NotNull byte[] compressedBytes = str.getBytes(ISO_8859_1);
        wire.write().compress("lzw", Bytes.wrapForRead(compressedBytes));

        @NotNull Bytes<?> bytes = bytesSupplier.get();
        wire.read().bytes(bytes);
        assertEquals(str, bytes.toString());
        bytes.releaseLast();
    }

    static void assertReadMetaData(Wire wire) {
        wire.bytes().append("---\n" +
                "!!meta-data\n" +
                "hello-world\n" +
                "...\n" +
                "---\n" +
                "!!data\n" +
                "hello-world\n" +
                "...\n" +
                "---\n" +
                "!!meta-data\n" +
                "dto: {\n" +
                "  text: hello-world\n" +
                "}\n" +
                "...\n" +
                "---\n" +
                "!!data\n" +
                "dto: {\n" +
                "  text: hello-world\n" +
                "}\n" +
                "...\n");
        for (int i = 0; i < 4; i++) {
            try (DocumentContext dc = wire.readingDocument()) {
                final boolean metaData = i % 2 == 0;
                assertEquals("i: " + i, metaData, dc.isMetaData());
            }
        }
    }

    static void assertAllCharsRoundTrip(Wire wire) {
        @NotNull char[] chars = new char[256];
        for (int i = 0; i < 1024; i++) {
            wire.clear();
            Arrays.fill(chars, (char) i);
            @NotNull String s = new String(chars);
            wire.writeDocument(false, w -> w.write(() -> "message").text(s));

            wire.readDocument(null, w -> w.read(() -> "message").text(s, Assert::assertEquals));
        }
    }

    static void writeDemarshallable(Wire wire) {
        try (DocumentContext ignored = wire.writingDocument(true)) {
            ignored.isData();
            wire.getValueOut().typedMarshallable(new DemarshallableObject("test", 12345));
        }
    }

    static String expectedDemarshallableBlob() {
        return "--- !!meta-data\n" +
                "!net.openhft.chronicle.wire.DemarshallableObject {\n" +
                "  name: test,\n" +
                "  value: 12345\n" +
                "}\n";
    }

    static void assertDemarshallableRead(Wire wire) {
        try (DocumentContext ignored = wire.readingDocument()) {
            ignored.isData();
            DemarshallableObject dobj = wire.getValueIn().typedMarshallable();
            assertEquals("test", dobj.name);
            assertEquals(12345, dobj.value);
        }
    }

    static void assertByteArrayValueWithSwapLeaf(Wire wire) {
        @NotNull final byte[] expected = {-1, -2, -3, -4, -5, -6, -7};
        wire.writeDocument(false, wir -> {
            ValueOut out = wir.writeEventName(() -> "put");
            out.swapLeaf(true);
            out.marshallable(w -> w.write(() -> "key")
                    .text("1")
                    .write(() -> "value")
                    .object(expected));
        });
        assertEquals("--- !!data\n" +
                        "put: { key: \"1\", value: !byte[] !!binary //79/Pv6+Q== }\n",
                Wires.fromSizePrefixedBlobs(wire.bytes()));

        wire.readDocument(null, wir -> wire.read(() -> "put")
                .marshallable(w -> w.read(() -> "key")
                        .object(Object.class, "1", Assert::assertEquals)
                        .read(() -> "value").object(byte[].class, expected, Assert::assertArrayEquals)));
    }

    static void assertTypeWithoutSpace(Wire wire) {
        wire.bytes().append("A: !").append(MyTypes.class.getName()).append("{}");

        @NotNull MyTypes mt = (MyTypes) wire.read(() -> "A").object();

        assertEquals("!net.openhft.chronicle.wire.MyTypes {\n" +
                "  text: \"\",\n" +
                "  flag: false,\n" +
                "  b: 0,\n" +
                "  s: 0,\n" +
                "  ch: \"\\0\",\n" +
                "  i: 0,\n" +
                "  f: 0.0,\n" +
                "  d: 0.0,\n" +
                "  l: 0\n" +
                "}\n", mt.toString());
    }

    static void assertNanValues(Wire wire) {
        wire.bytes().append(
                "A: NaN,\n" +
                        "A2: NaN ,\n" +
                        "A3: Infinity,\n" +
                        "A4: -Infinity,\n" +
                        "A5: NaN\n" +
                        "B: 1.23\n");

        assertEquals(Double.NaN, wire.read("A").float64(), 0);
        assertEquals(Double.NaN, wire.read("A2").float64(), 0);
        assertEquals(Double.POSITIVE_INFINITY, wire.read("A3").float64(), 0);
        assertEquals(Double.NEGATIVE_INFINITY, wire.read("A4").float64(), 0);
        assertEquals(Double.NaN, wire.read("A5").float64(), 0);
        assertEquals(1.23, wire.read("B").float64(), 0);
    }

    static void assertExceptionRoundTrip(Wire wire, String testClassName) {
        Exception e = new InvalidAlgorithmParameterException("Reference cannot be null") {
            private static final long serialVersionUID = 1L;
            @NotNull
            @Override
            public StackTraceElement[] getStackTrace() {
                @NotNull StackTraceElement[] stack = {
                        new StackTraceElement(testClassName, "testException", simpleName(testClassName) + ".java", 783),
                        new StackTraceElement(testClassName, "runTestException", simpleName(testClassName) + ".java", 73),
                        new StackTraceElement("sun.reflect.NativeMethodAccessorImpl", "invoke0", "NativeMethodAccessorImpl.java", -2)
                };
                return stack;
            }
        };

        wire.writeDocument(false, w -> w.writeEventName(() -> "exception").object(e));

        assertEquals("--- !!data\n" +
                        "exception: !" + e.getClass().getName() + " {\n" +
                        "  message: Reference cannot be null,\n" +
                        "  stackTrace: [\n" +
                        "    { class: " + testClassName + ", method: testException, file: " + simpleName(testClassName) + ".java, line: 783 },\n" +
                        "    { class: " + testClassName + ", method: runTestException, file: " + simpleName(testClassName) + ".java, line: 73 },\n" +
                        "    { class: sun.reflect.NativeMethodAccessorImpl, method: invoke0, file: NativeMethodAccessorImpl.java, line: -2 }\n" +
                        "  ]\n" +
                        "}\n",
                Wires.fromSizePrefixedBlobs(wire));

        wire.readDocument(null, r -> {
            Throwable t = r.read(() -> "exception").throwable(true);
            Assert.assertTrue(t instanceof InvalidAlgorithmParameterException);
        });
    }

    private static String simpleName(String className) {
        int pos = className.lastIndexOf('.');
        return pos == -1 ? className : className.substring(pos + 1);
    }
}
