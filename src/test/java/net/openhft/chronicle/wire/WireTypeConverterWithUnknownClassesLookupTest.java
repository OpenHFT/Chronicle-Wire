package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.internal.UnknownClassLookup;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WireTypeConverterWithUnknownClassesLookupTest { //extends WireTestCommon {
    @Test
    public void fromYamlToJsonAndBackToYaml() {
        WireTypeConverter wireTypeConverter = new WireTypeConverter(new UnknownClassLookup(ClassAliasPool.CLASS_ALIASES));
        Wires.GENERATE_TUPLES = true;
        Wires.THROW_CNFRE = false;
        String yaml = "!ChronicleServicesCfg {\n" +
                "  queues: {\n" +
                "    in: {\n" +
                "      path: tmp/benchmark/in\n" +
                "    },\n" +
                "    sender-one-out: {\n" +
                "      path: tmp/benchmark/sender-one-out,\n" +
                "      builder: !SingleChronicleQueueBuilder {\n" +
                "        useSparseFiles: true,\n" +
                "        rollCycle: HUGE_DAILY\n" +
                "      }\n" +
                "    },\n" +
                "    sender-two-out: {\n" +
                "      path: tmp/benchmark/sender-two-out,\n" +
                "      builder: !SingleChronicleQueueBuilder {\n" +
                "        useSparseFiles: true,\n" +
                "        rollCycle: HUGE_DAILY\n" +
                "      }\n" +
                "    },\n" +
                "    sender-three-out: {\n" +
                "      path: tmp/benchmark/sender-three-out,\n" +
                "      builder: !SingleChronicleQueueBuilder {\n" +
                "        useSparseFiles: true,\n" +
                "        rollCycle: HUGE_DAILY\n" +
                "      }\n" +
                "    },\n" +
                "    receiver-out: {\n" +
                "      path: tmp/benchmark/receiver-out\n" +
                "    }\n" +
                "  },\n" +
                "  services: {\n" +
                "    sender-one: {\n" +
                "      inputs: [\n" +
                "        in\n" +
                "      ],\n" +
                "      output: sender-one-out,\n" +
                "      startFromStrategy: $property.name,\n" +
                "      affinityCpu: any,\n" +
                "      pretouchMS: 100,\n" +
                "      serviceConfig: {\n" +
                "        param: !CustomClass1 {\n" +
                "          param2: value\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n";

        CharSequence json = wireTypeConverter.yamlToJson(yaml);

        CharSequence backToYaml = wireTypeConverter.jsonToYaml(json.toString());

        assertEquals(yaml, backToYaml.toString());
    }

    @Test
    public void typeReference() {
        WireTypeConverter wireTypeConverter = new WireTypeConverter(new UnknownClassLookup(ClassAliasPool.CLASS_ALIASES));
        Wires.GENERATE_TUPLES = true;
        Wires.THROW_CNFRE = false;
        String yaml = "!ChronicleServicesCfg {\n" +
                "  queues: {\n" +
                "    in: {\n" +
                "      path: tmp/benchmark/in\n" +
                "    },\n" +
                "    sender-one-out: {\n" +
                "      path: tmp/benchmark/sender-one-out,\n" +
                "      builder: !SingleChronicleQueueBuilder {\n" +
                "        useSparseFiles: true,\n" +
                "        rollCycle: HUGE_DAILY\n" +
                "      }\n" +
                "    },\n" +
                "    sender-two-out: {\n" +
                "      path: tmp/benchmark/sender-two-out,\n" +
                "      builder: !SingleChronicleQueueBuilder {\n" +
                "        useSparseFiles: true,\n" +
                "        rollCycle: HUGE_DAILY\n" +
                "      }\n" +
                "    },\n" +
                "    sender-three-out: {\n" +
                "      path: tmp/benchmark/sender-three-out,\n" +
                "      builder: !SingleChronicleQueueBuilder {\n" +
                "        useSparseFiles: true,\n" +
                "        rollCycle: HUGE_DAILY\n" +
                "      }\n" +
                "    },\n" +
                "    receiver-out: {\n" +
                "      path: tmp/benchmark/receiver-out\n" +
                "    }\n" +
                "  },\n" +
                "  services: {\n" +
                "    sender-one: {\n" +
                "      inputs: [\n" +
                "        in\n" +
                "      ],\n" +
                "      output: sender-one-out,\n" +
                "      startFromStrategy: START,\n" +
                "      affinityCpu: any,\n" +
                "      pretouchMS: 100,\n" +
                "      serviceConfig: {\n" +
                "        param: !CustomClass1 {\n" +
                "          param2: value\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n";

        CharSequence json = wireTypeConverter.yamlToJson(yaml);

        CharSequence backToYaml = wireTypeConverter.jsonToYaml(json.toString());

        assertEquals(yaml, backToYaml.toString());
    }
}
