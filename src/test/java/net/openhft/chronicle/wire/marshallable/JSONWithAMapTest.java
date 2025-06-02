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

package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.OnHeapBytes;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

import static net.openhft.chronicle.core.pool.ClassAliasPool.CLASS_ALIASES;

public class JSONWithAMapTest extends net.openhft.chronicle.wire.WireTestCommon {
    @Test
    public void test1() {
        final String expected = "{\"@ResponseItem\":{\"index\":\"4ab100000005\",\"key\":\"seqNumber\",\"payload\":null}}";

        final String input = "!ResponseItem {\n" +
                "  index: \"4ab100000005\",\n" +
                "  key: seqNumber,\n" +
                "}";

        doTest(expected, input);
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

        doTest(expected, input);
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

        doTest(expected, input);
    }

    private void doTest(String expected, String input) {
        CLASS_ALIASES.addAlias(ResponseItem.class);
        ResponseItem responseItem = Marshallable.fromString(ResponseItem.class, input);

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
        Assert.assertEquals(expected, actual);
    }

    static class ResponseItem extends SelfDescribingMarshallable {
        @NotNull String index;
        Bytes<?> key = Bytes.allocateElasticOnHeap();
        private Object payload;
    }
}
