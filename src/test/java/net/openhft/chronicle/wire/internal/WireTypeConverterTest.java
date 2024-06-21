package net.openhft.chronicle.wire.internal;

import net.openhft.chronicle.wire.WireTypeConverter;
import org.junit.Assert;
import org.junit.Test;

public class WireTypeConverterTest {

    private final String json = "" +
            "{\"@FixEngineCfg\":{\"SERVER-CLIENT\":{\"connectionType\":\"initiator\",\n" +
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

    private final String yaml = "" +
            "!FixEngineCfg {\n" +
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
    public void testYamlToJson() {
        Assert.assertEquals(json,
                new WireTypeConverter().yamlToJson(yaml).toString().replaceAll(",", ",\n"));
    }

    @Test
    public void testJsonToYaml() {
        Assert.assertEquals(yaml, new WireTypeConverter().jsonToYaml(json).toString());
    }
}
