//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//
package net.openhft.chronicle.wire;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

// Class to test JSON schema generation
public class GenerateJsonSchemaMainTest extends WireTestCommon {

    // Test method to verify schema generation for a given class
    @Test
    public void generateSchemaFor() throws ClassNotFoundException {
        // Generate the schema for the ITop class
        final String main0 = GenerateJsonSchemaMain.main0(ITop.class.getName());

        // Compare the generated schema to the expected schema
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
