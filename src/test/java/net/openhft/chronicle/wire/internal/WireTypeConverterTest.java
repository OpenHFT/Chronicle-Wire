package net.openhft.chronicle.wire.internal;
import org.junit.Assert;
import org.junit.Test;

public class WireTypeConverterTest {

    private final String json = "{\"@net.openhft.chronicle.wire.internal.MyClass\":{\"msg\":\"msg:\\\"hello\\\"\"}}";
    private final String yaml = "!net.openhft.chronicle.wire.internal.MyClass {\n" +
            "  msg: \"msg:\\\"hello\\\"\"\n" +
            "}\n";

    private final String jsonUnknownClass = "{\"@net.openhft.chronicle.wire.internal.UnknownClass\":{\"msg\":\"msg:\\\"hello\\\"\"}}";
    private final String yamlUnknownClass = "!net.openhft.chronicle.wire.internal.UnknownClass {\n" +
            "  msg: \"msg:\\\"hello\\\"\"\n" +
            "}\n";

    @Test
    public void testYamlToJson() throws Exception {
        Assert.assertEquals(json, new WireTypeConverter().yamlToJson(yaml).toString());
        Assert.assertEquals(yaml, new WireTypeConverter().jsonToYaml(json).toString());
    }

    @Test
    public void testJsonToYaml() throws Exception {
        Assert.assertEquals(yaml, new WireTypeConverter().jsonToYaml(json).toString());
        Assert.assertEquals(json, new WireTypeConverter().yamlToJson(yaml).toString());
    }


    @Test(expected = net.openhft.chronicle.core.util.ClassNotFoundRuntimeException.class)
    public void testYamlToJsonUnknownClass() throws Exception {
        Assert.assertEquals(jsonUnknownClass, new WireTypeConverter().yamlToJson(yamlUnknownClass).toString());
        Assert.assertEquals(yamlUnknownClass, new WireTypeConverter().jsonToYaml(jsonUnknownClass).toString());
    }

    @Test(expected = net.openhft.chronicle.core.util.ClassNotFoundRuntimeException.class)
    public void testJsonToYamlUnknownClass() throws Exception {
        Assert.assertEquals(yamlUnknownClass, new WireTypeConverter().jsonToYaml(jsonUnknownClass).toString());
        Assert.assertEquals(jsonUnknownClass, new WireTypeConverter().yamlToJson(yamlUnknownClass).toString());
    }

    @Test(expected = ClassCastException.class)
    public void testYamlClassCastException() throws Exception {
        new WireTypeConverter().yamlToJson("!net.openhft.chronicle.wire.internal.MyClass2 {\n" +
                "  myClass: !net.openhft.chronicle.wire.internal.MyClass2 { }\n" +
                "}\n");
    }

    @Test(expected = ClassCastException.class)
    public void testJsonClassCastException() throws Exception {
        new WireTypeConverter().jsonToYaml("\"@net.openhft.chronicle.wire.internal.MyClass2\": {\n" +
                " myClass: {\"@net.openhft.chronicle.wire.internal.MyClass2\": { } }\n" +
                "}\n");
    }

}
