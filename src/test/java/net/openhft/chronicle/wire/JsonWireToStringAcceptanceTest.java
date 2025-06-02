/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
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
package net.openhft.chronicle.wire;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Verify that unicode characters can be properly represented in JSON output.
 */
public class JsonWireToStringAcceptanceTest {

    private static Collection<WireType> WIRE_TYPES = Arrays.asList(WireType.JSON, WireType.JSON_ONLY);

    @ParameterizedTest
    @ValueSource(strings = {"£", "€", "¥", "₹", "ó", "óaóó", "☞☞☞☞☞", "ÊÆÄ"})
    public void json_verifyAsString(String input) {
        Map<String, String> map = new HashMap<>();
        map.put("x", input);
        for (WireType wireType : WIRE_TYPES) {
            assertEquals("{\"x\":\"" + input + "\"}", wireType.asString(map));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"£", "€", "¥", "₹", "ó", "óaóó"})
    public void json_verifyObjectToString(String input) {
        Map<String, String> map = new HashMap<>();
        map.put("x", input);
        WireOut object = new JSONWire().getValueOut().object(map);
        assertEquals("{\"x\":\"" + input + "\"}", object.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"£", "€", "¥", "₹", "ó", "óaóó"})
    public void json_verifyAsText(String input) {
        Map<String, String> map = new HashMap<>();
        map.put("x", input);
        JSONWire jsonWire = new JSONWire();
        jsonWire.getValueOut().object(map);
        assertEquals("{\"x\":\"" + input + "\"}", JSONWire.asText(jsonWire));
    }

}