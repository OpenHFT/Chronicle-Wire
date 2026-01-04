/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.internal;

import net.openhft.chronicle.wire.WireTypeConverter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WireTypeConverterTest {

    private static final String JSON = "{\"@FixEngineCfg\":{\"SERVER-CLIENT\":{\"connectionType\":\"initiator\",\n" +
            "\"connectionStrategy\":{\"@AlwaysStartOnPrimaryConnectionStrategy\":{}},\n" +
            "\"senderCompID\":\"CLIENT\",\n" +
            "\"fixVersion\":\"V4_4\",\n" +
            "\"heartBtInt\":2,\n" +
            "\"targetCompID\":\"SERVER\",\n" +
            "\"fileStorePath\":\"fix/initiator\",\n" +
            "\"socketConnectHostPort\":[\"host.port\" ],\n" +
            "\"messageParser\":{\"@MessageParser\":{}},\n" +
            "\"messageNotifier\":{\"@software.chronicle.platform.fix.gui.endtoend.notifer.InitiatorMessageNotifier\":{\"i\":0,\n" +
            "\"clordID\":0}},\n" +
            "\"messageGenerator\":{\"@software.chronicle.fix50sp2.generators.MessageGenerator\":{}},\n" +
            "\"loggingMode\":\"UNBUFFERED\",\n" +
            "\"hostId\":2,\n" +
            "\"msgSequenceHandler\":{\"@QueueMsgSequenceHandler\":{\"recordIncoming\":false}},\n" +
            "\"autoLogon\":true,\n" +
            "\"compIdValidation\":\"strict\"}}}";

    private static final String YAML = "!FixEngineCfg {\n" +
            "  SERVER-CLIENT: {\n" +
            "    connectionType: initiator,\n" +
            "    connectionStrategy: !AlwaysStartOnPrimaryConnectionStrategy { },\n" +
            "    senderCompID: CLIENT,\n" +
            "    fixVersion: V4_4,\n" +
            "    heartBtInt: 2,\n" +
            "    targetCompID: SERVER,\n" +
            "    fileStorePath: fix/initiator,\n" +
            "    socketConnectHostPort: [\n" +
            "      host.port\n" +
            "    ],\n" +
            "    messageParser: !MessageParser { },\n" +
            "    messageNotifier: !software.chronicle.platform.fix.gui.endtoend.notifer.InitiatorMessageNotifier { i: 0, clordID: 0 },\n" +
            "    messageGenerator: !software.chronicle.fix50sp2.generators.MessageGenerator { },\n" +
            "    loggingMode: UNBUFFERED,\n" +
            "    hostId: 2,\n" +
            "    msgSequenceHandler: !QueueMsgSequenceHandler { recordIncoming: false },\n" +
            "    autoLogon: true,\n" +
            "    compIdValidation: strict\n" +
            "  }\n" +
            "}\n";

    @Test
    @DisplayName("YAML conversion should produce JSON output")
    void testYamlToJson() {
        Assertions.assertEquals(JSON,
                new WireTypeConverter().yamlToJson(YAML).toString().replaceAll(",", ",\n"),
                "YAML to JSON conversion should match expected output");
    }

    @Test
    @DisplayName("JSON conversion should produce YAML output")
    void testJsonToYaml() {
        Assertions.assertEquals(YAML, new WireTypeConverter().jsonToYaml(JSON).toString(),
                "JSON to YAML conversion should match expected output");
    }
}
