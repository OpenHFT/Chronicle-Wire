/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.DocumentContext;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;

import java.util.Arrays;
import java.security.InvalidAlgorithmParameterException;
import java.util.function.Supplier;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

@SuppressWarnings("deprecation")
final class WireTestSupport {
    private WireTestSupport() {
    }

    static void assertWireTypeRoundTrip(Wire wire, String expectedText) {
        wire.write().object(WireType.BINARY)
                .write().object(WireType.TEXT)
                .write().object(WireType.RAW);

        Assertions.assertEquals(expectedText,
                wire.toString(),
                "Wire output should match expected round trip text");

        Assertions.assertEquals(WireType.BINARY,
                wire.read().object(Object.class),
                "Wire should read back BINARY type token");
        Assertions.assertEquals(WireType.TEXT,
                wire.read().object(Object.class),
                "Wire should read back TEXT type token");
        Assertions.assertEquals(WireType.RAW,
                wire.read().object(Object.class),
                "Wire should read back RAW type token");
    }

    static void assertLzwCompressionAsText(Wire wire, Supplier<Bytes<?>> bytesSupplier) {
        @NotNull final String s = "xxxxxxxxxxxxxxxxxxx2xxxxxxxxxxxxxxxxxxxxxxx";
        @NotNull String str = s + s + s + s;
        @NotNull byte[] compressedBytes = str.getBytes(ISO_8859_1);
        wire.write().compress("lzw", Bytes.wrapForRead(compressedBytes));

        @NotNull Bytes<?> bytes = bytesSupplier.get();
        wire.read().bytes(bytes);
        Assertions.assertEquals(str,
                bytes.toString(),
                "LZW compression should round trip full text");
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
                Assertions.assertEquals(metaData,
                        dc.isMetaData(),
                        "Metadata flag should match document index " + i);
            }
        }
    }

    static void assertAllCharsRoundTrip(Wire wire) {
        @NotNull char[] chars = new char[256];
        for (int i = 0; i < 1024; i++) {
            final int index = i;
            wire.clear();
            Arrays.fill(chars, (char) i);
            @NotNull String s = new String(chars);
            wire.writeDocument(false, w -> w.write(() -> "message").text(s));

            wire.readDocument(null, w -> w.read(() -> "message")
                    .text(s, (expected, actual) -> Assertions.assertEquals(expected,
                            actual,
                            "Character block should round-trip at i=" + index)));
        }
    }

    static void writeDemarshallable(Wire wire) {
        try (DocumentContext dc = wire.writingDocument(true)) {
            dc.wire().getValueOut().typedMarshallable(new DemarshallableObject("test", 12345));
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
        try (DocumentContext dc = wire.readingDocument()) {
            DemarshallableObject dobj = dc.wire().getValueIn().typedMarshallable();
            Assertions.assertEquals("test",
                    dobj.name,
                    "Demarshalled name should match expected value");
            Assertions.assertEquals(12345,
                    dobj.value,
                    "Demarshalled value should match expected number");
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
        Assertions.assertEquals("--- !!data\n" +
                        "put: { key: \"1\", value: !byte[] !!binary //79/Pv6+Q== }\n",
                Wires.fromSizePrefixedBlobs(wire.bytes()),
                "Swap leaf should serialise expected binary blob");

        wire.readDocument(null, wir -> wire.read(() -> "put")
                .marshallable(w -> w.read(() -> "key")
                        .object(Object.class,
                                "1",
                                (exp, act) -> Assertions.assertEquals(exp,
                                        act,
                                        "Swap leaf should preserve key value"))
                        .read(() -> "value").object(byte[].class,
                                expected,
                                (exp, act) -> Assertions.assertArrayEquals(exp,
                                        act,
                                        "Swap leaf should preserve byte array value"))));
    }

    static void assertTypeWithoutSpace(Wire wire) {
        wire.bytes().append("A: !").append(MyTypes.class.getName()).append("{}");

        @NotNull MyTypes mt = (MyTypes) wire.read(() -> "A").object();

        Assertions.assertEquals("!net.openhft.chronicle.wire.MyTypes {\n" +
                "  text: \"\",\n" +
                "  flag: false,\n" +
                "  b: 0,\n" +
                "  s: 0,\n" +
                "  ch: \"\\0\",\n" +
                "  i: 0,\n" +
                "  f: 0.0,\n" +
                "  d: 0.0,\n" +
                "  l: 0\n" +
                "}\n",
                mt.toString(),
                "Type without space should parse default fields");
    }

    static void assertNanValues(Wire wire) {
        wire.bytes().append(
                "A: NaN,\n" +
                        "A2: NaN ,\n" +
                        "A3: Infinity,\n" +
                        "A4: -Infinity,\n" +
                        "A5: NaN\n" +
                        "B: 1.23\n");

        Assertions.assertEquals(Double.NaN,
                wire.read("A").float64(),
                0,
                "NaN parse should match field A value");
        Assertions.assertEquals(Double.NaN,
                wire.read("A2").float64(),
                0,
                "NaN parse should match field A2 entry");
        Assertions.assertEquals(Double.POSITIVE_INFINITY,
                wire.read("A3").float64(),
                0,
                "Infinity parse should match field A3 value");
        Assertions.assertEquals(Double.NEGATIVE_INFINITY,
                wire.read("A4").float64(),
                0,
                "Negative infinity parse should match field A4 value");
        Assertions.assertEquals(Double.NaN,
                wire.read("A5").float64(),
                0,
                "NaN parse should match field A5 value");
        Assertions.assertEquals(1.23,
                wire.read("B").float64(),
                0,
                "Numeric parse should match field B value");
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

        Assertions.assertEquals("--- !!data\n" +
                        "exception: !" + e.getClass().getName() + " {\n" +
                        "  message: Reference cannot be null,\n" +
                        "  stackTrace: [\n" +
                        "    { class: " + testClassName + ", method: testException, file: " + simpleName(testClassName) + ".java, line: 783 },\n" +
                        "    { class: " + testClassName + ", method: runTestException, file: " + simpleName(testClassName) + ".java, line: 73 },\n" +
                        "    { class: sun.reflect.NativeMethodAccessorImpl, method: invoke0, file: NativeMethodAccessorImpl.java, line: -2 }\n" +
                        "  ]\n" +
                        "}\n",
                Wires.fromSizePrefixedBlobs(wire),
                "Exception should serialise with expected stack trace");

        wire.readDocument(null, r -> {
            Throwable t = r.read(() -> "exception").throwable(true);
            Assertions.assertInstanceOf(InvalidAlgorithmParameterException.class,
                    t,
                    "Decoded exception should be InvalidAlgorithmParameterException type");
        });
    }

    private static String simpleName(String className) {
        int pos = className.lastIndexOf('.');
        return pos == -1 ? className : className.substring(pos + 1);
    }
}
