/*
 * Copyright 2016-2025 chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
