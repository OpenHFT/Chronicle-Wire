/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.OnHeapBytes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static net.openhft.chronicle.core.pool.ClassAliasPool.CLASS_ALIASES;

// relates to https://github.com/OpenHFT/Chronicle-Wire/issues/467
public class TestJsonIssue467 {

    static class ResponseItem467 extends SelfDescribingMarshallable {
        public String index;
        public final Bytes<?> key = Bytes.allocateElasticOnHeap();
        public Object payload;
    }

    @Test
    public void test() {
        assertResponseItemJson();
    }

    @Test
    public void test2() {
        assertResponseItemJson();
    }

    private void assertResponseItemJson() {
        CLASS_ALIASES.addAlias(ResponseItem467.class);

        ResponseItem467 responseItem467 = Marshallable.fromString(ResponseItem467.class, "!ResponseItem467 {\n" +
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
        jsonWire.getValueOut().object(responseItem467);

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
        Assertions.assertEquals(openBracket, closeBracket, "openBracket and closeBracket should match");

        // DON'T CHANGE THE EXPECTED JSON IT IS CORRECT ! - please use this website to validate the json - https://jsonformatter.org
        Assertions.assertEquals("{\"@ResponseItem467\":{\"index\":\"4ab100000005\",\"key\":\"seqNumber\",\"payload\":{\"eventId\":\"periodicUpdate\",\"eventTime\":1652109920838805734,\"seqNumbers\":[ {\"sessionID\":{\"localCompID\":\"SERVER\",\"remoteCompID\":\"CLIENT\",\"localSubID\":null,\"remoteSubID\":null},\"rSeq\":1517,\"wSeq\":1519,\"isActive\":true,\"isConnected\":false} ]}}}", actual);
    }

    private static Wire jsonResponseItem() {
        CLASS_ALIASES.addAlias(ResponseItem467.class);
        String json = " {\"@ResponseItem467\":{\"index\":\"4dc800000034\",\"key\":\"notificationMsg\",\"payload\":\"Successfully debited your account by 0.0\"}}";
        return WireType.JSON_ONLY.apply(Bytes.from(json));
    }

    @Test
    public void testWireObject() {
        final Wire jsonWire = jsonResponseItem();
        ResponseItem467 responseItem467 = jsonWire.getValueIn().object(ResponseItem467.class);

        Assertions.assertEquals("!ResponseItem467 {\n" +
                "  index: \"4dc800000034\",\n" +
                "  key: notificationMsg,\n" +
                "  payload: Successfully debited your account by 0.0\n" +
                "}\n", responseItem467.toString());
    }

    @Test
    public void testWireReusingObject() {
        final Wire jsonWire = jsonResponseItem();
        ResponseItem467 responseItem4671 = new ResponseItem467();
        ResponseItem467 responseItem467 = jsonWire.getValueIn().object(responseItem4671, ResponseItem467.class);
        Assertions.assertEquals("!ResponseItem467 {\n" +
                "  index: \"4dc800000034\",\n" +
                "  key: notificationMsg,\n" +
                "  payload: Successfully debited your account by 0.0\n" +
                "}\n", responseItem467.toString());
    }
}
