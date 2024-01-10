package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.internal.UnknownClassLookup;
import org.junit.Test;

public class WireTypeConverterWithUnknownClassesLookupTest extends WireTestCommon {

    @Test
    public void fromYamlToJsonAndBackToYaml() {
        WireTypeConverter wireTypeConverter = new WireTypeConverter(new UnknownClassLookup(ClassAliasPool.CLASS_ALIASES));
        String yaml = "!ChronicleServicesCfg {\n" +
            "  queues: {\n" +
            "    in: { path: tmp/benchmark/in },\n" +
            "    sender-one-out: { path: tmp/benchmark/sender-one-out, builder: !SingleChronicleQueueBuilder { useSparseFiles: true, rollCycle: HUGE_DAILY } },\n" +
            "    sender-two-out: { path: tmp/benchmark/sender-two-out, builder: !SingleChronicleQueueBuilder { useSparseFiles: true, rollCycle: HUGE_DAILY } },\n" +
            "    sender-three-out: { path: tmp/benchmark/sender-three-out, builder: !SingleChronicleQueueBuilder { useSparseFiles: true, rollCycle: HUGE_DAILY } },\n" +
            "    receiver-out: { path: tmp/benchmark/receiver-out },\n" +
            "  },\n" +
            "  services: {\n" +
            "    sender-one: {\n" +
            "      inputs: [ in ],\n" +
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
            "}";
        CharSequence json = wireTypeConverter.yamlToJson(yaml);
        System.out.println(json);

        CharSequence backToYaml = wireTypeConverter.jsonToYaml(json);
        System.out.println(backToYaml);
    }

    @Test
    public void typeReference() {
        WireTypeConverter wireTypeConverter = new WireTypeConverter(new UnknownClassLookup(ClassAliasPool.CLASS_ALIASES));
        String yaml = "!ChronicleServicesCfg {\n" +
            "  queues: {\n" +
            "    in: { path: tmp/benchmark/in },\n" +
            "    sender-one-out: { path: tmp/benchmark/sender-one-out, builder: !SingleChronicleQueueBuilder { useSparseFiles: true, rollCycle: HUGE_DAILY } },\n" +
            "    sender-two-out: { path: tmp/benchmark/sender-two-out, builder: !SingleChronicleQueueBuilder { useSparseFiles: true, rollCycle: HUGE_DAILY } },\n" +
            "    sender-three-out: { path: tmp/benchmark/sender-three-out, builder: !SingleChronicleQueueBuilder { useSparseFiles: true, rollCycle: HUGE_DAILY } },\n" +
            "    receiver-out: { path: tmp/benchmark/receiver-out },\n" +
            "  },\n" +
            "  services: {\n" +
            "    sender-one: {\n" +
            "      inputs: [ in ],\n" +
            "      output: sender-one-out,\n" +
            "      startFromStrategy: START,\n" +
            "      affinityCpu: any,\n" +
            "      pretouchMS: 100,\n" +
            "      implClass: !type non.existing.package.SenderOneService,\n" +
            "      serviceConfig: {\n" +
            "        param: !CustomClass1 {\n" +
            "          param2: value\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "}";

        CharSequence json = wireTypeConverter.yamlToJson(yaml);
        System.out.println(json);

        CharSequence backToYaml = wireTypeConverter.jsonToYaml(json);
        System.out.println(backToYaml);
    }
}
