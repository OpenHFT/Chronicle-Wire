package net.openhft.chronicle.wire;

import org.junit.Assert;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class WireTypeConverterTest extends net.openhft.chronicle.wire.WireTestCommon {

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
    public void testYamlToJson() {
        Assert.assertEquals(json, new WireTypeConverter().yamlToJson(yaml).toString());
        Assert.assertEquals(yaml, new WireTypeConverter().jsonToYaml(json).toString());
    }

    @Test
    public void testJsonToYaml() {
        Assert.assertEquals(yaml, new WireTypeConverter().jsonToYaml(json).toString());
        Assert.assertEquals(json, new WireTypeConverter().yamlToJson(yaml).toString());
    }

    @Test
    public void testYamlToJsonUnknownClass() throws Exception {
        Assert.assertEquals(jsonUnknownClass, new WireTypeConverter().yamlToJson(yamlUnknownClass).toString());
        Assert.assertEquals(yamlUnknownClass, new WireTypeConverter().jsonToYaml(jsonUnknownClass).toString());
    }

    @Test
    public void testJsonToYamlUnknownClass() {
        Assert.assertEquals(yamlUnknownClass, new WireTypeConverter().jsonToYaml(jsonUnknownClass).toString());
        Assert.assertEquals(jsonUnknownClass, new WireTypeConverter().yamlToJson(yamlUnknownClass).toString());
    }

    @Test
    public void testYamlClassCastException() {
        CharSequence yamlToJson = new WireTypeConverter().yamlToJson(
                "!net.openhft.chronicle.wire.MyClass2 {\n" +
                        "  myClass: !net.openhft.chronicle.wire.MyClass2 { x: aa }\n" +
                        "}\n");
        assertEquals("" +
                        "{\"@net.openhft.chronicle.wire.MyClass2\":{\"myClass\":{\"@net.openhft.chronicle.wire.MyClass2\":{\"x\":\"aa\"}}}}",
                yamlToJson.toString());

    }

    @Test
    public void testYamlNoClassCastException() {
        final WireTypeConverter converter = new WireTypeConverter();
        converter.addAlias(MyClass3.class, "net.openhft.chronicle.wire.MyOldClass");
        final CharSequence json = converter.yamlToJson(
                "!net.openhft.chronicle.wire.MyClass2 {\n" +
                        "  myClass: !net.openhft.chronicle.wire.MyOldClass { x: abc }\n" +
                        "}\n");
        assertEquals("{\"@net.openhft.chronicle.wire.MyClass2\":{\"myClass\":{\"@MyOldClass\":{\"x\":\"abc\"}}}}",
                json.toString());
    }

    @Test
    public void testJsonClassCastException() {
        CharSequence jsonToYaml = new WireTypeConverter().jsonToYaml(
                "{\"@net.openhft.chronicle.wire.MyClass2\": {\n" +
                        " \"myClass\": {\"@net.openhft.chronicle.wire.MyClass2\": { \"x\": \"bb\" } }\n" +
                        "} }\n");
        assertEquals("" +
                        "!net.openhft.chronicle.wire.MyClass2 {\n" +
                        "  myClass: !net.openhft.chronicle.wire.MyClass2 {\n" +
                        "    x: bb\n" +
                        "  }\n" +
                        "}\n",
                jsonToYaml.toString());
    }

    @Test
    public void testJsonNoClassCastException() {
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
