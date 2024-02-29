package net.openhft.chronicle.wire.internal;

import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireTypeConverter;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WireTypeConverterWithTuplesTest extends WireTestCommon {
    @Test
    public void fromYamlToJsonAndBackToYaml() {
        WireTypeConverter wireTypeConverter = new WireTypeConverter();
        String originalYaml = "" +
                "!ChronicleServicesCfg {\n" +
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

        CharSequence json = wireTypeConverter.yamlToJson(originalYaml);
        String expected = "" +
                "{\"@ChronicleServicesCfg\":{\"queues\":{\"in\":{\"path\":\"tmp/benchmark/in\"},\n" +
                "\"sender-one-out\":{\"path\":\"tmp/benchmark/sender-one-out\",\n" +
                "\"builder\":{\"@SingleChronicleQueueBuilder\":{\"useSparseFiles\":\"true\",\n" +
                "\"rollCycle\":\"HUGE_DAILY\"}},\n" +
                "\"sender-two-out\":{\"path\":\"tmp/benchmark/sender-two-out\",\n" +
                "\"builder\":{\"@SingleChronicleQueueBuilder\":{\"useSparseFiles\":\"true\",\n" +
                "\"rollCycle\":\"HUGE_DAILY\"}},\n" +
                "\"sender-three-out\":{\"path\":\"tmp/benchmark/sender-three-out\",\n" +
                "\"builder\":{\"@SingleChronicleQueueBuilder\":{\"useSparseFiles\":\"true\",\n" +
                "\"rollCycle\":\"HUGE_DAILY\"}},\n" +
                "\"receiver-out\":{\"path\":\"tmp/benchmark/receiver-out\"}},\n" +
                "\"services\":{\"sender-one\":{\"inputs\":[\"in\" ],\n" +
                "\"output\":\"sender-one-out\",\n" +
                "\"startFromStrategy\":\"$property.name\",\n" +
                "\"affinityCpu\":\"any\",\n" +
                "\"pretouchMS\":100,\n" +
                "\"serviceConfig\":{\"param\":{\"@CustomClass1\":{\"param2\":\"value\"}}}}}}}}}}";
        assertEquals(expected,
                json.toString().replace(",", ",\n"));

        CharSequence jsonToYaml = wireTypeConverter.jsonToYaml(json.toString());

        assertEquals("" +
                "!ChronicleServicesCfg {\n" +
                "  queues: {\n" +
                "    in: { path: tmp/benchmark/in },\n" +
                "    sender-one-out: { path: tmp/benchmark/sender-one-out, builder: !SingleChronicleQueueBuilder { useSparseFiles: true, rollCycle: HUGE_DAILY }, sender-two-out: { path: tmp/benchmark/sender-two-out, builder: !SingleChronicleQueueBuilder { useSparseFiles: true, rollCycle: HUGE_DAILY }, sender-three-out: { path: tmp/benchmark/sender-three-out, builder: !SingleChronicleQueueBuilder { useSparseFiles: true, rollCycle: HUGE_DAILY }, receiver-out: { path: tmp/benchmark/receiver-out } }, services: { sender-one: { inputs: [ in ], output: sender-one-out, startFromStrategy: $property.name, affinityCpu: any, pretouchMS: 100,serviceConfig: { param: !CustomClass1 { param2: value } } } } } }\n" +
                "  }\n" +
                "}\n", jsonToYaml.toString());
    }
}
