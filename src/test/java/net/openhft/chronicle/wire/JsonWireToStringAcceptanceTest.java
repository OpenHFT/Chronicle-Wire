/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Verify that unicode characters can be properly represented in JSON output.
 */
@SuppressWarnings({"UnnecessaryUnicodeEscape", "deprecation", "removal"})
class JsonWireToStringAcceptanceTest {

    private static final Collection<WireType> WIRE_TYPES = Arrays.asList(WireType.JSON, WireType.JSON_ONLY);

    @ParameterizedTest
    @ValueSource(strings = {"£", "€", "¥", "\u20B9", "ó", "óaóó", "", "ÊÆÄ"})
    void json_verifyAsString(String input) {
        Map<String, String> map = new HashMap<>();
        map.put("x", input);
        for (WireType wireType : WIRE_TYPES) {
            Assertions.assertEquals("{\"x\":\"" + input + "\"}", wireType.asString(map));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"£", "€", "¥", "\u20B9", "ó", "óaóó"})
    void json_verifyObjectToString(String input) {
        Map<String, String> map = new HashMap<>();
        map.put("x", input);
        WireOut object = new JSONWire().getValueOut().object(map);
        Assertions.assertEquals("{\"x\":\"" + input + "\"}", object.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"£", "€", "¥", "\u20B9", "ó", "óaóó"})
    void json_verifyAsText(String input) {
        Map<String, String> map = new HashMap<>();
        map.put("x", input);
        JSONWire jsonWire = new JSONWire();
        jsonWire.getValueOut().object(map);
        Assertions.assertEquals("{\"x\":\"" + input + "\"}", JSONWire.asText(jsonWire));
    }

}
