/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.WireIn;
import net.openhft.chronicle.wire.WireOut;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MarshallingJSONStringTest implements Marshallable {

    private String configAsJSON;

    @Override
    public void writeMarshallable(@NotNull final WireOut wire) throws InvalidMarshallableException {
        wire.write("config").text(configAsJSON);
    }

    @Override
    public void readMarshallable(@NotNull final WireIn wire) throws IORuntimeException, InvalidMarshallableException {
        configAsJSON = wire.read("config").text();
    }

    @Test
    @DisplayName("JSON content should remain without type prefix")
    public void testNoPrefixAddedToJson() {

        String configJson = "!net.openhft.chronicle.wire.marshallable.MarshallingJSONStringTest {\n" +
                "  config: {\n" +
                "    \"username\": \"sampleApp\",\n" +
                "    \"password\": \"samplePassword\",\n" +
                "    \"publishPort\": 4021,\n" +
                "    \"subscribePort\": 4024,\n" +
                "  }\n" +
                "}";
        String expectedJson = "{\n" +
        "    \"username\": \"sampleApp\",\n" +
                "    \"password\": \"samplePassword\",\n" +
                "    \"publishPort\": 4021,\n" +
                "    \"subscribePort\": 4024,\n" +
                "  }";

        MarshallingJSONStringTest read = Marshallable.fromString(configJson);
        assertEquals(expectedJson, read.configAsJSON, "JSON content should be preserved without a type prefix");
    }
}
