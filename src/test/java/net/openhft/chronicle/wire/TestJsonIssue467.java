/*
 * Copyright 2016-2022 chronicle.software
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

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.OnHeapBytes;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import static net.openhft.chronicle.core.pool.ClassAliasPool.CLASS_ALIASES;

// relates to https://github.com/OpenHFT/Chronicle-Wire/issues/467
public class TestJsonIssue467 {

    public static class ResponseItem extends SelfDescribingMarshallable {
        @NotNull
        String index;
        private final Bytes<?> key = Bytes.elasticByteBuffer();
        private Object payload;
    }

    @Ignore
    @Test
    public void test() {
        CLASS_ALIASES.addAlias(ResponseItem.class);

        ResponseItem responseItem = Marshallable.fromString(ResponseItem.class, "!ResponseItem {\n" +
                "  index: \"4ab100000005\",\n" +
                "  key: seqNumber,\n" +
                "  payload: {\n" +
                "    eventId: periodicUpdate,\n" +
                "    eventTime: 1652109920838805734,\n" +
                "    seqNumbers: [\n" +
                "      {\n" +
                "        sessionID: {\n" +
                "          localCompID: SERVER,\n" +
                "          remoteCompID: CLIENT,\n" +
                "          localSubID: !!null \"\",\n" +
                "          remoteSubID: !!null \"\"\n" +
                "        },\n" +
                "        rSeq: !short 1517,\n" +
                "        wSeq: !short 1519,\n" +
                "        isActive: true,\n" +
                "        isConnected: false\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}");

        OnHeapBytes buffer = Bytes.allocateElasticOnHeap();
        final Wire jsonWire = WireType.JSON_ONLY.apply(buffer);
        jsonWire.getValueOut().object(responseItem);

        String actual = buffer.toString();

        int openBracket = 0;
        int closeBracket = 0;
        for (int i = 0; i < actual.length(); i++) {
            if (actual.charAt(i) == '{')
                openBracket++;
            if (actual.charAt(i) == '}')
                closeBracket++;
        }

        // check the number of '{' match the number of '}'
        Assert.assertTrue("openBracket=" + openBracket + ",closeBracket=" + closeBracket, openBracket == closeBracket);

        // DON'T CHANGE THE EXPECTED JSON IT IS CORRECT ! - please use this website to validate the json - https://jsonformatter.org
        Assert.assertEquals("{\"@ResponseItem\":{\"index\":\"4ab100000005\",\"key\":\"seqNumber\",\"payload\":{\"eventId\":\"periodicUpdate\",\"eventTime\":1652109920838805734,\"seqNumbers\":[ {\"sessionID\":{\"localCompID\":\"SERVER\",\"remoteCompID\":\"CLIENT\",\"localSubID\":null,\"remoteSubID\":null},\"rSeq\":1517,\"wSeq\":1519,\"isActive\":true,\"isConnected\":false} ]}}}", actual);
    }

    @Ignore
    @Test
    public void test2() {
        CLASS_ALIASES.addAlias(ResponseItem.class);

        ResponseItem responseItem = Marshallable.fromString(ResponseItem.class, "!ResponseItem {\n" +
                "  index: \"4ab100000005\",\n" +
                "  key: seqNumber,\n" +
                "  payload: {\n" +
                "    eventId: periodicUpdate,\n" +
                "    eventTime: 1652109920838805734,\n" +
                "    seqNumbers: [\n" +
                "      {\n" +
                "        sessionID: {\n" +
                "          localCompID: SERVER,\n" +
                "          remoteCompID: CLIENT,\n" +
                "          localSubID: !!null \"\",\n" +
                "          remoteSubID: !!null \"\"\n" +
                "        },\n" +
                "        rSeq: !short 1517,\n" +
                "        wSeq: !short 1519,\n" +
                "        isActive: true,\n" +
                "        isConnected: false\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}");

        OnHeapBytes buffer = Bytes.allocateElasticOnHeap();
        final Wire jsonWire = WireType.JSON_ONLY.apply(buffer);
        jsonWire.getValueOut().object(responseItem);

        String actual = buffer.toString();

        int openBracket = 0;
        int closeBracket = 0;
        for (int i = 0; i < actual.length(); i++) {
            if (actual.charAt(i) == '{')
                openBracket++;
            if (actual.charAt(i) == '}')
                closeBracket++;
        }

        // check the number of '{' match the number of '}'
        Assert.assertTrue("openBracket=" + openBracket + ",closeBracket=" + closeBracket, openBracket == closeBracket);

        // DON'T CHANGE THE EXPECTED JSON IT IS CORRECT ! - please use this website to validate the json - https://jsonformatter.org
        Assert.assertEquals("{\"@ResponseItem\":{\"index\":\"4ab100000005\",\"key\":\"seqNumber\",\"payload\":{\"eventId\":\"periodicUpdate\",\"eventTime\":1652109920838805734,\"seqNumbers\":[ {\"sessionID\":{\"localCompID\":\"SERVER\",\"remoteCompID\":\"CLIENT\",\"localSubID\":null,\"remoteSubID\":null},\"rSeq\":1517,\"wSeq\":1519,\"isActive\":true,\"isConnected\":false} ]}}}", actual);
    }

    private static Wire jsonResponseItem() {
        CLASS_ALIASES.addAlias(ResponseItem.class);
        String json = " {\"@ResponseItem\":{\"index\":\"4dc800000034\",\"key\":\"notificationMsg\",\"payload\":\"Successfully debited your account by 0.0\"}}";
        final Wire jsonWire = WireType.JSON_ONLY.apply(Bytes.from(json));
        return jsonWire;
    }

    @Ignore
    @Test
    public void testWireObject() {
        final Wire jsonWire = jsonResponseItem();
        ResponseItem responseItem = jsonWire.getValueIn().object(ResponseItem.class);

        Assert.assertEquals("!ResponseItem {\n" +
                "  index: \"4dc800000034\",\n" +
                "  key: notificationMsg,\n" +
                "  payload: Successfully debited your account by 0.0\n" +
                "}\n", responseItem.toString());
    }

    @Ignore("to do fix - https://github.com/OpenHFT/Chronicle-Wire/issues/920")
    @Test
    public void testWireReusingObject() {
        final Wire jsonWire = jsonResponseItem();
        ResponseItem responseItem1 = new ResponseItem();
        ResponseItem responseItem = jsonWire.getValueIn().object(responseItem1, ResponseItem.class);
        Assert.assertEquals("!ResponseItem {\n" +
                "  index: \"4dc800000034\",\n" +
                "  key: notificationMsg,\n" +
                "  payload: Successfully debited your account by 0.0\n" +
                "}\n", responseItem.toString());
    }


}
