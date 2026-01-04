/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings({"deprecation", "removal"})
public class WireTypeConverterTest extends net.openhft.chronicle.wire.WireTestCommon {

    private static final String JSON =
            "{\"@net.openhft.chronicle.wire.MyClass\":{\"msg\":\"msg:\\\"hello\\\"\"}}";

    private static final String YAML =
            "!net.openhft.chronicle.wire.MyClass {\n" +
                    "  msg: \"msg:\\\"hello\\\"\"\n" +
                    "}\n";

    private static final String JSON_UNKNOWN_CLASS =
            "{\"@net.openhft.chronicle.wire.UnknownClass\":{\"msg\":\"msg:\\\"hello\\\"\"}}";
    private static final String YAML_UNKNOWN_CLASS =
            "!net.openhft.chronicle.wire.UnknownClass {\n" +
                    "  msg: \"msg:\\\"hello\\\"\"\n" +
                    "}\n";

    @Test
    @DisplayName("YAML converts to JSON and back")
    public void testYamlToJson() {
        assertEquals(JSON,
                new WireTypeConverter().yamlToJson(YAML).toString(),
                "YAML to JSON conversion should match expected output for known class");
        assertEquals(YAML,
                new WireTypeConverter().jsonToYaml(JSON).toString(),
                "JSON to YAML conversion should match expected output for known class");
    }

    @Test
    @DisplayName("JSON converts to YAML and back")
    public void testJsonToYaml() {
        assertEquals(YAML,
                new WireTypeConverter().jsonToYaml(JSON).toString(),
                "JSON to YAML conversion should match expected output from JSON input");
        assertEquals(JSON,
                new WireTypeConverter().yamlToJson(YAML).toString(),
                "YAML to JSON conversion should match expected output from YAML input");
    }

    @Test
    @DisplayName("Unknown YAML classes convert to JSON")
    public void testYamlToJsonUnknownClass() {
        assertEquals(JSON_UNKNOWN_CLASS,
                new WireTypeConverter().yamlToJson(YAML_UNKNOWN_CLASS).toString(),
                "Unknown YAML to JSON conversion should match output for unknown class");
        assertEquals(YAML_UNKNOWN_CLASS,
                new WireTypeConverter().jsonToYaml(JSON_UNKNOWN_CLASS).toString(),
                "Unknown JSON to YAML conversion should match output for unknown class");
    }

    @Test
    @DisplayName("Unknown JSON classes convert to YAML")
    public void testJsonToYamlUnknownClass() {
        assertEquals(YAML_UNKNOWN_CLASS,
                new WireTypeConverter().jsonToYaml(JSON_UNKNOWN_CLASS).toString(),
                "Unknown JSON to YAML conversion should match output from JSON input");
        assertEquals(JSON_UNKNOWN_CLASS,
                new WireTypeConverter().yamlToJson(YAML_UNKNOWN_CLASS).toString(),
                "Unknown YAML to JSON conversion should match output from YAML input");
    }

    @Test
    @DisplayName("YAML conversion handles nested class casting")
    public void testYamlClassCastException() {
        CharSequence yamlToJson = new WireTypeConverter().yamlToJson(
                "!net.openhft.chronicle.wire.MyClass2 {\n" +
                        "  myClass: !net.openhft.chronicle.wire.MyClass2 { x: aa }\n" +
                        "}\n");
        assertEquals("{\"@net.openhft.chronicle.wire.MyClass2\":{\"myClass\":{\"@net.openhft.chronicle.wire.MyClass2\":{\"x\":\"aa\"}}}}",
                yamlToJson.toString(),
                "Nested YAML conversion should match expected JSON");

    }

    @Test
    @DisplayName("YAML conversion uses aliases without casting errors")
    public void testYamlNoClassCastException() {
        final WireTypeConverter converter = new WireTypeConverter();
        converter.addAlias(MyClass3.class, "net.openhft.chronicle.wire.MyOldClass");
        final CharSequence json = converter.yamlToJson(
                "!net.openhft.chronicle.wire.MyClass2 {\n" +
                        "  myClass: !net.openhft.chronicle.wire.MyOldClass { x: abc }\n" +
                        "}\n");
        assertEquals("{\"@net.openhft.chronicle.wire.MyClass2\":{\"myClass\":{\"@MyOldClass\":{\"x\":\"abc\"}}}}",
                json.toString(),
                "Aliased YAML conversion should match expected JSON");
    }

    @Test
    @DisplayName("JSON conversion handles nested class casting")
    public void testJsonClassCastException() {
        CharSequence jsonToYaml = new WireTypeConverter().jsonToYaml(
                "{\"@net.openhft.chronicle.wire.MyClass2\": {\n" +
                        " \"myClass\": {\"@net.openhft.chronicle.wire.MyClass2\": { \"x\": \"bb\" } }\n" +
                        "} }\n");
        assertEquals("!net.openhft.chronicle.wire.MyClass2 {\n" +
                        "  myClass: !net.openhft.chronicle.wire.MyClass2 {\n" +
                        "    x: bb\n" +
                        "  }\n" +
                        "}\n",
                jsonToYaml.toString(),
                "Nested JSON conversion should match expected YAML");
    }

    @Test
    @DisplayName("JSON conversion uses aliases without casting errors")
    public void testJsonNoClassCastException() {
        final WireTypeConverter converter = new WireTypeConverter();
        converter.addAlias(MyClass3.class, "MyOldClass");
        final CharSequence yaml = converter.jsonToYaml("{\"@net.openhft.chronicle.wire.MyClass2\": {\n" +
                "  \"myClass\": {\"@MyOldClass\": { \"x\": \"abcd\" } }\n" +
                "} }\n");
        assertEquals("!net.openhft.chronicle.wire.MyClass2 {\n" +
                "  myClass: !MyOldClass {\n" +
                "    x: abcd\n" +
                "  }\n" +
                "}\n",
                yaml.toString(),
                "Aliased JSON conversion should match expected YAML");
    }
}
