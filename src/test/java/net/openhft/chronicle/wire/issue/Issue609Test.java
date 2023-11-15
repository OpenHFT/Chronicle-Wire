/*
 * Copyright 2016-2020 chronicle.software
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
package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.*;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

import static net.openhft.chronicle.core.util.StringUtils.isEqual;
import static org.junit.Assert.assertEquals;

/**
 * Test class extending `WireTestCommon` to verify the deserialization of `ChronicleServicesCfg` from YAML.
 */
public class Issue609Test extends WireTestCommon {

    /**
     * Tests the deserialization of services from a YAML file and ensures that the deserialized object
     * matches the expected configuration.
     *
     * @throws IOException if there's an error reading the file.
     */
    @Test
    public void testServices() throws IOException {
        // Deserializes the ChronicleServicesCfg from a YAML file
        ChronicleServicesCfg obj = WireType.YAML.fromString(ChronicleServicesCfg.class, BytesUtil.readFile("yaml/services.yaml"));

        // Creates an expected configuration manually
        ChronicleServicesCfg expected = new ChronicleServicesCfg();

        ServiceCfg scfg = new ServiceCfg();
        expected.services.put("fix-web-gateway", scfg);

        // Setting up expected service inputs
        scfg.inputs.add(new InputCfg().input("web-gateway-periodic-updates"));
        scfg.inputs.add(new InputCfg().input("session-state-updates"));
        scfg.inputs.add(new InputCfg().input("fix-config-out"));
        scfg.inputs.add(new InputCfg().input("fix-search-out"));

        // Asserts that the deserialized object matches the expected configuration
        assertEquals(expected, obj);
    }

    /**
     * Configuration class representing a collection of services.
     */
    public static class ChronicleServicesCfg extends AbstractMarshallableCfg {
        public final Map<String, ServiceCfg> services = new LinkedHashMap<>();
    }

    /**
     * Configuration class representing a specific service.
     * This class also includes custom deserialization logic to properly deserialize
     * the 'inputs' field, which can have different value types.
     */
    public static class ServiceCfg extends AbstractMarshallableCfg {
        public final List<InputCfg> inputs = new ArrayList<>();

        @SuppressWarnings("unchecked")
        @Override
        public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
            @NotNull StringBuilder name = new StringBuilder();
            while (wire.hasMore()) {
                @NotNull ValueIn in = wire.read(name);
                if (isEqual(name, "inputs")) {
                    in.sequence(inputs, Object.class, (inputCfgs, inputCfgClass, valueIn) -> {
                        while (valueIn.hasNextSequenceItem()) {
                            Bytes<?> bytes = wire.bytes();
                            long pos = bytes.readPosition();
                            Object cfg0 = valueIn.typedMarshallable();
                            InputCfg cfg;
                            if (cfg0 instanceof InputCfg) {
                                cfg = (InputCfg) cfg0;
                            } else if (cfg0 instanceof Map) {
                                bytes.readPosition(pos);
                                valueIn.marshallable(cfg = new InputCfg());
                            } else if (cfg0 instanceof String) {
                                cfg = new InputCfg().input((String) cfg0);
                            } else {
                                bytes.readPosition(pos);
                                cfg = new InputCfg().input(valueIn.text());
                            }
                            inputCfgs.add(cfg);
                        }
                    });
                    if (new HashSet<>(inputs).size() != inputs.size())
                        throw new IllegalArgumentException("inputs not unique: " + inputs);
                } else {
                    in.typedMarshallable();
                }
                wire.consumePadding();
            }
        }
    }

    /**
     * Configuration class representing a single input of a service.
     */
    public static class InputCfg extends AbstractMarshallableCfg {
        private String input;

        /**
         * Sets the input and returns the instance.
         *
         * @param input the name of the input.
         * @return this instance.
         */
        public InputCfg input(String input) {
            this.input = input;
            return this;
        }
    }
}
