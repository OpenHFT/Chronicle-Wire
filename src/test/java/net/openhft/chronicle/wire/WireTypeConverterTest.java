/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WireTypeConverterTest extends net.openhft.chronicle.wire.WireTestCommon {

    private final String json =
            "{\"@net.openhft.chronicle.wire.MyClass\":{\"msg\":\"msg:\\\"hello\\\"\"}}";

    private final String yaml =
            "!net.openhft.chronicle.wire.MyClass {\n" +
                    "  msg: \"msg:\\\"hello\\\"\"\n" +
                    "}\n";

    private final String jsonUnknownClass =
            "{\"@net.openhft.chronicle.wire.UnknownClass\":{\"msg\":\"msg:\\\"hello\\\"\"}}";
    private final String yamlUnknownClass =
            "!net.openhft.chronicle.wire.UnknownClass {\n" +
                    "  msg: \"msg:\\\"hello\\\"\"\n" +
                    "}\n";

    @Test
    void testYamlToJson() {
        assertEquals(json, new WireTypeConverter().yamlToJson(yaml).toString());
        assertEquals(yaml, new WireTypeConverter().jsonToYaml(json).toString());
    }

    @Test
    void testJsonToYaml() {
        assertEquals(yaml, new WireTypeConverter().jsonToYaml(json).toString());
        assertEquals(json, new WireTypeConverter().yamlToJson(yaml).toString());
    }

    @Test
    void testYamlToJsonUnknownClass() throws Exception {
        assertEquals(jsonUnknownClass, new WireTypeConverter().yamlToJson(yamlUnknownClass).toString());
        assertEquals(yamlUnknownClass, new WireTypeConverter().jsonToYaml(jsonUnknownClass).toString());
    }

    @Test
    void testJsonToYamlUnknownClass() {
        assertEquals(yamlUnknownClass, new WireTypeConverter().jsonToYaml(jsonUnknownClass).toString());
        assertEquals(jsonUnknownClass, new WireTypeConverter().yamlToJson(yamlUnknownClass).toString());
    }

    @Test
    void testYamlClassCastException() {
        CharSequence yamlToJson = new WireTypeConverter().yamlToJson(
                "!net.openhft.chronicle.wire.MyClass2 {\n" +
                        "  myClass: !net.openhft.chronicle.wire.MyClass2 { x: aa }\n" +
                        "}\n");
        assertEquals("" +
                "{\"@net.openhft.chronicle.wire.MyClass2\":{\"myClass\":{\"@net.openhft.chronicle.wire.MyClass2\":{\"x\":\"aa\"}}}}", yamlToJson.toString());

    }

    @Test
    void testYamlNoClassCastException() {
        final WireTypeConverter converter = new WireTypeConverter();
        converter.addAlias(MyClass3.class, "net.openhft.chronicle.wire.MyOldClass");
        final CharSequence json = converter.yamlToJson(
                "!net.openhft.chronicle.wire.MyClass2 {\n" +
                        "  myClass: !net.openhft.chronicle.wire.MyOldClass { x: abc }\n" +
                        "}\n");
        assertEquals("{\"@net.openhft.chronicle.wire.MyClass2\":{\"myClass\":{\"@MyOldClass\":{\"x\":\"abc\"}}}}", json.toString());
    }

    @Test
    void testJsonClassCastException() {
        CharSequence jsonToYaml = new WireTypeConverter().jsonToYaml(
                "{\"@net.openhft.chronicle.wire.MyClass2\": {\n" +
                        " \"myClass\": {\"@net.openhft.chronicle.wire.MyClass2\": { \"x\": \"bb\" } }\n" +
                        "} }\n");
        assertEquals("" +
                "!net.openhft.chronicle.wire.MyClass2 {\n" +
                "  myClass: !net.openhft.chronicle.wire.MyClass2 {\n" +
                "    x: bb\n" +
                "  }\n" +
                "}\n", jsonToYaml.toString());
    }

    @Test
    void testJsonNoClassCastException() {
        final WireTypeConverter converter = new WireTypeConverter();
        converter.addAlias(MyClass3.class, "MyOldClass");
        final CharSequence yaml = converter.jsonToYaml("" +
                "{\"@net.openhft.chronicle.wire.MyClass2\": {\n" +
                "  \"myClass\": {\"@MyOldClass\": { \"x\": \"abcd\" } }\n" +
                "} }\n");
        assertEquals("" +
                "!net.openhft.chronicle.wire.MyClass2 {\n" +
                "  myClass: !MyOldClass {\n" +
                "    x: abcd\n" +
                "  }\n" +
                "}\n", yaml.toString());
    }
}
