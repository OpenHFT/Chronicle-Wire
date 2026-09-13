/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class BooleanScalarTypePreservationTest extends WireTestCommon {

    private static final WireType[] TEXT_FORMATS = {WireType.TEXT, WireType.YAML_ONLY, WireType.JSON};

    @Test
    public void textAndTypedBooleansRetainTypes() {
        for (WireType wireType : TEXT_FORMATS) {
            assertScalar(wireType, "true", String.class, out -> out.text("true"));
            assertScalar(wireType, true, Boolean.class, out -> out.bool(true));
            assertScalar(wireType, "false", String.class, out -> out.text("false"));
            assertScalar(wireType, false, Boolean.class, out -> out.bool(false));
        }
    }

    @Test
    public void quotedJsonBooleansRemainStrings() {
        assertParsedString(WireType.JSON, "\"true\"", "true");
        assertParsedString(WireType.JSON, "\"false\"", "false");
    }

    @Test
    public void singleQuotedBooleansRemainStrings() {
        for (WireType wireType : new WireType[]{WireType.TEXT, WireType.YAML_ONLY}) {
            assertParsedString(wireType, "'true'", "true");
            assertParsedString(wireType, "'false'", "false");
        }
    }

    @Test
    public void mixedListRetainsTypes() {
        List<Object> expected = Arrays.<Object>asList("true", true, "false", false, true, "true");

        for (WireType wireType : TEXT_FORMATS)
            assertRoundTrip(wireType, expected);
    }

    @Test
    public void nestedMapsAndListsRetainTypes() {
        Map<String, Object> child = new LinkedHashMap<>();
        child.put("text", "false");
        child.put("flag", false);

        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("before", true);
        expected.put("items", Arrays.<Object>asList("true", true, child, "false", false));
        expected.put("afterText", "true");
        expected.put("afterFlag", true);

        for (WireType wireType : TEXT_FORMATS)
            assertRoundTrip(wireType, expected);
    }

    @Test
    public void quotedNestedInputRetainsTypes() {
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("items", Arrays.<Object>asList("true", true, "false", false));
        expected.put("after", true);

        // TextWire's flow scanner requires separator whitespace.
        String input = "{\"items\": [\"true\", true, \"false\", false], \"after\": true}";
        for (WireType wireType : TEXT_FORMATS)
            assertParsed(wireType, input, expected);

        for (WireType wireType : new WireType[]{WireType.YAML_ONLY, WireType.JSON})
            assertParsed(wireType, input.replace(" ", ""), expected);

        for (WireType wireType : new WireType[]{WireType.TEXT, WireType.YAML_ONLY})
            assertParsed(wireType, input.replace('"', '\''), expected);

        assertParsed(WireType.YAML_ONLY,
                "items:\n" +
                        "  - 'true'\n" +
                        "  - true\n" +
                        "  - \"false\"\n" +
                        "  - false\n" +
                        "after: true\n",
                expected);
    }

    @Test
    public void booleanLookingMapKeysRemainStrings() {
        Map<Object, Object> expected = new LinkedHashMap<>();
        expected.put("true", "false");
        expected.put("false", true);

        for (WireType wireType : TEXT_FORMATS) {
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            try {
                wireType.apply(bytes).getValueOut().marshallable(expected);
                String output = bytes.toString();
                assertTrue(wireType + ": " + output, Pattern.compile("\"true\"\\s*:").matcher(output).find());
                assertTrue(wireType + ": " + output, Pattern.compile("\"false\"\\s*:").matcher(output).find());

                Map<Object, Object> actual = wireType.apply(bytes).getValueIn().marshallableAsMap(Object.class, Object.class);
                assertEquals(wireType.name(), expected, actual);
                for (Object key : actual.keySet())
                    assertSame(wireType.name(), String.class, key.getClass());
            } finally {
                bytes.releaseLast();
            }
        }
    }

    @Test
    public void nestedDtoObjectFieldsRetainTypes() {
        for (WireType wireType : TEXT_FORMATS) {
            Envelope expected = new Envelope();
            expected.child = new Child();
            expected.child.textTrue = "true";
            expected.child.flagTrue = true;
            expected.child.textFalse = "false";
            expected.child.flagFalse = false;

            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            try {
                wireType.apply(bytes).getValueOut().marshallable(expected);
                Envelope actual = wireType.apply(bytes).getValueIn().object(Envelope.class);
                assertNotNull(wireType.name(), actual);
                assertNotNull(wireType.name(), actual.child);
                assertValueAndType(wireType, "true", String.class, actual.child.textTrue);
                assertValueAndType(wireType, true, Boolean.class, actual.child.flagTrue);
                assertValueAndType(wireType, "false", String.class, actual.child.textFalse);
                assertValueAndType(wireType, false, Boolean.class, actual.child.flagFalse);
            } finally {
                bytes.releaseLast();
            }
        }
    }

    private static void assertRoundTrip(WireType wireType, Object expected) {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            wireType.apply(bytes).getValueOut().object(expected);
            assertEquals(wireType.name(), expected, wireType.apply(bytes).getValueIn().object());
        } finally {
            bytes.releaseLast();
        }
    }

    private static void assertParsed(WireType wireType, String input, Object expected) {
        Bytes<?> bytes = Bytes.from(input);
        try {
            assertEquals(wireType.name() + ": " + input, expected, wireType.apply(bytes).getValueIn().object());
        } finally {
            bytes.releaseLast();
        }
    }

    private static void assertScalar(@NotNull WireType wireType, Object expected, @NotNull Class<?> expectedType,
                                     @NotNull Consumer<ValueOut> writer) {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            writer.accept(wireType.apply(bytes).getValueOut());
            assertValueAndType(wireType, expected, expectedType, wireType.apply(bytes).getValueIn().object());
        } finally {
            bytes.releaseLast();
        }
    }

    private static void assertParsedString(@NotNull WireType wireType, @NotNull String input, @NotNull String expected) {
        Bytes<?> bytes = Bytes.from(input);
        try {
            assertValueAndType(wireType, expected, String.class, wireType.apply(bytes).getValueIn().object());
        } finally {
            bytes.releaseLast();
        }
    }

    private static void assertValueAndType(WireType wireType, Object expected, @NotNull Class<?> expectedType, Object actual) {
        assertEquals(wireType.name(), expected, actual);
        assertSame(wireType.name(), expectedType, actual.getClass());
    }

    public static class Envelope extends SelfDescribingMarshallable {
        public Child child;
    }

    public static class Child extends SelfDescribingMarshallable {
        public Object textTrue;
        public Object flagTrue;
        public Object textFalse;
        public Object flagFalse;
    }
}
