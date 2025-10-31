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
package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.WireIn;
import net.openhft.chronicle.wire.WireOut;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

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
        assertEquals(expectedJson, read.configAsJSON);
    }
}
