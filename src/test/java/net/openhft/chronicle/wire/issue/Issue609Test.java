/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.issue;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

import static net.openhft.chronicle.core.util.StringUtils.isEqual;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Regression coverage for issue 609: verifies ChronicleServicesCfg deserialisation
 * from YAML services configuration sources.
 */
@SuppressWarnings({"deprecation", "removal"})
class Issue609Test extends WireTestCommon {

    /**
     * Tests the deserialisation of services from a YAML file and ensures that the deserialised object
     * matches the expected configuration.
     *
     * @throws IOException if there is an error reading the file
     */
    @Test
    @DisplayName("YAML services config should deserialise correctly")
    void testServices() throws IOException {
        // Deserialises the ChronicleServicesCfg from a YAML file
        final ChronicleServicesCfg obj = WireType.YAML.fromString(ChronicleServicesCfg.class, BytesUtil.readFile("yaml/services.yaml"));

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
        assertEquals(expected, obj, "Deserialised services config should match expected");
    }

    @Test
    @DisplayName("Services config round-trips across wire types")
    void toYamlAndBackIssue824() {
        ChronicleServicesCfg expected = new ChronicleServicesCfg();

        ServiceCfg scfg = new ServiceCfg();
        expected.services.put("fix-web-gateway", scfg);

        scfg.inputs.add(new InputCfg().input("web-gateway-periodic-updates"));

        String yaml = WireType.YAML_ONLY.asString(expected);

        System.out.println(yaml);

        assertEquals(expected, WireType.TEXT.fromString(yaml),
                "TEXT wire should parse YAML output correctly");

        assertEquals(expected, WireType.YAML_ONLY.fromString(yaml),
                "YAML_ONLY wire should parse YAML output correctly");

        String withString = "!net.openhft.chronicle.wire.issue.Issue609Test$ChronicleServicesCfg {\n" +
                "  services: {\n" +
                "    fix-web-gateway: { inputs: [ 'web-gateway-periodic-updates' ] }\n" +
                "  }\n" +
                "}";

        assertEquals(expected, WireType.YAML_ONLY.fromString(withString),
                "YAML_ONLY wire should parse explicit class name YAML");
        assertEquals(expected, WireType.TEXT.fromString(withString),
                "TEXT wire should parse explicit class name YAML");

        String withString2 = withString.replace("'", "");
        assertEquals(expected, WireType.YAML_ONLY.fromString(withString2),
                "YAML_ONLY wire should parse YAML without quotes");
        assertEquals(expected, WireType.TEXT.fromString(withString2),
                "TEXT wire should parse YAML without quotes");
    }

    static class ChronicleServicesCfg extends AbstractMarshallableCfg {
        final Map<String, ServiceCfg> services = new LinkedHashMap<>();
    }

    /**
     * Configuration class representing a specific service.
     * This class also includes custom deserialization logic to properly deserialize
     * the 'inputs' field, which can have different value types.
     */
    static class ServiceCfg extends AbstractMarshallableCfg {
        final List<InputCfg> inputs = new ArrayList<>();

        @Override
        public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
            @NotNull StringBuilder name = new StringBuilder();
            while (wire.hasMore()) {
                @NotNull ValueIn in = wire.read(name);
                if (isEqual(name, "inputs")) {
                    in.sequence(inputs, Object.class, (inputCfgs, inputCfgClass, valueIn) -> {
                        while (valueIn.hasNextSequenceItem()) {
                            InputCfg cfg = valueIn.object(InputCfg.class);
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
    @SuppressFBWarnings(value = "URF_UNREAD_FIELD", justification = "Field is read by marshalling configuration")
    public static class InputCfg extends AbstractMarshallableCfg {
        String input;

        InputCfg() {
            this(null);
        }

        InputCfg(String in) {
            this.input = in;
        }

        InputCfg input(String input) {
            this.input = input;
            return this;
        }
    }
}
