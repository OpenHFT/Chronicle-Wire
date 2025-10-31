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
