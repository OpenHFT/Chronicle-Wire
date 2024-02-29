package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Issue844Test extends WireTestCommon {

    @Test
    public void nestedMapsJson() {

        // at least 3 levels of nested to reproduce this issue
        Object o2 = WireType.JSON_ONLY.fromString("\"serviceConfig\": {\n" +
                "  \"db\": {\n" +
                "    \"a\": {\n" +
                "      \"Hello\": \"World\"\n" +
                "    },\n" +
                "    \"mongodb\": {\n" +
                "      \"@net.openhft.chronicle.wire.issue.Issue844Test$Enum\": \"INSTANCE\"\n" +
                "    },\n" +
                "    \"collection\": \"ladder\"\n" +
                "  }\n" +
                "}");
        assertEquals(
                "serviceConfig: {\n" +
                        "  db: {\n" +
                        "    a: {\n" +
                        "      Hello: World\n" +
                        "    },\n" +
                        "    mongodb: !net.openhft.chronicle.wire.issue.Issue844Test$Enum INSTANCE,\n" +
                        "    collection: ladder\n" +
                        "  }\n" +
                        "}\n",
                WireType.YAML_ONLY.asString(o2));
        assertEquals(
                "{\"serviceConfig\":{\"db\":{\"a\":{\"Hello\":\"World\"},\"mongodb\":{\"@net.openhft.chronicle.wire.issue.Issue844Test$Enum\":\"INSTANCE\"},\"collection\":\"ladder\"}}}",
                WireType.JSON_ONLY.asString(o2)
        );
    }


    public enum Enum {
        INSTANCE
    }

}
