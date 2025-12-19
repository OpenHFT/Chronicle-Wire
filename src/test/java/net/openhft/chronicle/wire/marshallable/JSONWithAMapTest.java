/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.OnHeapBytes;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static net.openhft.chronicle.core.pool.ClassAliasPool.CLASS_ALIASES;

public class JSONWithAMapTest extends net.openhft.chronicle.wire.WireTestCommon {
    @Test
    public void test1() {
        final String expected = "{\"@ResponseItem\":{\"index\":\"4ab100000005\",\"key\":\"seqNumber\",\"payload\":null}}";

        final String input = "!ResponseItem {\n" +
                "  index: \"4ab100000005\",\n" +
                "  key: seqNumber,\n" +
                "}";

        String actual = toJson(input);
        Assertions.assertEquals(expected, actual, "jsonWithAMap: test1");
    }

    @Test
    public void test2() {
        final String input = "!ResponseItem {\n" +
                "  index: \"4ab100000005\",\n" +
                "  key: seqNumber,\n" +
                "  payload: {\n" +
                "  }\n" +
                "}";
        final String expected = "{\"@ResponseItem\":{\"index\":\"4ab100000005\",\"key\":\"seqNumber\",\"payload\":{}}}";

        String actual = toJson(input);
        Assertions.assertEquals(expected, actual, "jsonWithAMap: test2");
    }

    @Test
    public void test5() {

        final String input = "!ResponseItem {\n" +
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
                "}";
        final String expected = "{\"@ResponseItem\":{\"index\":\"4ab100000005\",\"key\":\"seqNumber\",\"payload\":{\"eventId\":\"periodicUpdate\",\"eventTime\":1652109920838805734,\"seqNumbers\":[ {\"sessionID\":{\"localCompID\":\"SERVER\",\"remoteCompID\":\"CLIENT\",\"localSubID\":null,\"remoteSubID\":null},\"rSeq\":1517,\"wSeq\":1519,\"isActive\":true,\"isConnected\":false} ]}}}";

        String actual = toJson(input);
        Assertions.assertEquals(expected, actual, "jsonWithAMap: test5");
    }

    private String toJson(String input) {
        CLASS_ALIASES.addAlias(ResponseItem.class);
        ResponseItem responseItem = Marshallable.fromString(ResponseItem.class, input);

        OnHeapBytes buffer = Bytes.allocateElasticOnHeap();
        try {
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
            Assertions.assertEquals(openBracket, closeBracket, "openBracket=" + openBracket + ",closeBracket=" + closeBracket);

            return actual;
        } finally {
            buffer.releaseLast();
        }
    }

    private static class ResponseItem extends SelfDescribingMarshallable {
        public String index;
        public Bytes<?> key = Bytes.allocateElasticOnHeap();
        public Object payload;
    }
}
