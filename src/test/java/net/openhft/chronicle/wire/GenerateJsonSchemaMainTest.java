/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GenerateJsonSchemaMainTest extends WireTestCommon {

    @Test
    public void generateSchemaFor() throws ClassNotFoundException {
        final String main0 = GenerateJsonSchemaMain.main0(ITop.class.getName());
        assertEquals("" +
                "{\n" +
                "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
                "  \"$id\": \"http://json-schema.org/draft-07/schema#\",\n" +
                "  \"title\": \"Core schema meta-schema\",\n" +
                "  \"definitions\": {\n" +
                "    \"DMOuterClass\": {\n" +
                "      \"type\": \"object\",\n" +
                "      \"required\": [\n" +
                "      \"b\",\n" +
                "      \"bb\",\n" +
                "      \"s\",\n" +
                "      \"f\",\n" +
                "      \"d\",\n" +
                "      \"l\",\n" +
                "      \"i\"\n" +
                "      ],\n" +
                "      \"properties\": {\n" +
                "        \"text\": {\n" +
                "          \"type\": \"string\"\n" +
                "        },\n" +
                "        \"b\": {\n" +
                "          \"type\": \"boolean\"\n" +
                "        },\n" +
                "        \"bb\": {\n" +
                "          \"type\": \"integer\"\n" +
                "        },\n" +
                "        \"s\": {\n" +
                "          \"type\": \"integer\"\n" +
                "        },\n" +
                "        \"f\": {\n" +
                "          \"type\": \"number\"\n" +
                "        },\n" +
                "        \"d\": {\n" +
                "          \"type\": \"number\"\n" +
                "        },\n" +
                "        \"l\": {\n" +
                "          \"type\": \"integer\"\n" +
                "        },\n" +
                "        \"i\": {\n" +
                "          \"type\": \"integer\"\n" +
                "        },\n" +
                "        \"nested\": {\n" +
                "          \"type\": \"array\"\n" +
                "        },\n" +
                "        \"map\": {\n" +
                "          \"type\": \"object\"\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"properties\": {\n" +
                "    \"dto\": {\n" +
                "      \"$ref\": \"#/definitions/DMOuterClass\"\n" +
                "    },\n" +
                "    \"echo\": {\n" +
                "      \"type\": \"string\"\n" +
                "    },\n" +
                "    \"mid\": {\n" +
                "      \"type\": \"string\"\n" +
                "    },\n" +
                "    \"mid2\": {\n" +
                "      \"type\": \"string\"\n" +
                "    },\n" +
                "    \"midNoArg\": {\n" +
                "      \"type\": \"constant\",\n" +
                "    },\n" +
                "    \"midTwoArgs\": {\n" +
                "    },\n" +
                "    \"next\": {\n" +
                "      \"type\": \"integer\"\n" +
                "    },\n" +
                "    \"next2\": {\n" +
                "      \"type\": \"string\"\n" +
                "    },\n" +
                "  }\n" +
                "}\n", main0);
    }
}
