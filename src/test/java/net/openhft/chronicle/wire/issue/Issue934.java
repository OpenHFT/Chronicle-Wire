package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.wire.JSONWire;
import net.openhft.chronicle.wire.WireType;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Verify that unicode characters can be properly represented in JSON output.
 */
public class Issue934 {

    @ParameterizedTest
    @ValueSource(strings = {"£", "€", "¥", "₹", "ó", "óaóó", "☞☞☞☞☞", "ÊÆÄ"})
    public void json_verifyAsString(String input) {
        Map<String, String> map = new HashMap<>();
        map.put("x", input);
        assertEquals("{\"x\":\"" + input + "\"}", WireType.JSON.asString(map));
    }

    @ParameterizedTest
    @ValueSource(strings = {"£", "€", "¥", "₹", "ó", "óaóó"})
    public void json_verifyObjectToString(String input) {
        Map<String, String> map = new HashMap<>();
        map.put("x", input);
        assertEquals("{\"x\":\"" + input + "\"}", new JSONWire().getValueOut().object(map).toString());
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
