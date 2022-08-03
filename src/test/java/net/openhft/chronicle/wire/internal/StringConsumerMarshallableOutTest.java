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

package net.openhft.chronicle.wire.internal;

import net.openhft.chronicle.wire.MarshallableOut;
import net.openhft.chronicle.wire.WireType;
import org.junit.Test;

import java.io.StringWriter;

import static org.junit.Assert.assertEquals;

public class StringConsumerMarshallableOutTest {
    @Test
    public void saysYaml() {
        final WireType wireType = WireType.YAML_ONLY;
        final String expected = "" +
                "say: One\n" +
                "...\n" +
                "say: Two\n" +
                "...\n" +
                "say: Three\n" +
                "...\n";
        doTest(wireType, expected);
    }

    @Test
    public void saysJson() {
        final WireType wireType = WireType.JSON_ONLY;
        final String expected = "" +
                "\"say\":\"One\"\n" +
                "\"say\":\"Two\"\n" +
                "\"say\":\"Three\"\n";
        doTest(wireType, expected);
    }

    private void doTest(WireType wireType, String expected) {
        StringWriter sw = new StringWriter();
        MarshallableOut out = new StringConsumerMarshallableOut(s -> {
            sw.append(s);
            if (!s.endsWith("\n"))
                sw.append('\n');
        }, wireType);
        final Says says = out.methodWriter(Says.class);
        says.say("One");
        says.say("Two");
        says.say("Three");
        assertEquals(expected,
                sw.toString());
    }

    interface Says {
        void say(String text);
    }
}