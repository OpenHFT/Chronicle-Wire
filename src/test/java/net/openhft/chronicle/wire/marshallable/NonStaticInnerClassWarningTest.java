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

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class NonStaticInnerClassWarningTest extends WireTestCommon {

    @Test
    public void warnIfMarshallableHasAnOuterClass() {
        expectException("Found this$0, in class net.openhft.chronicle.wire.marshallable.NonStaticInnerClassWarningTest$Inner which will be ignore");
        Inner in = new Inner();
        in.hello = "World";

        String asText = in.toString();
        assertEquals("" +
                "!net.openhft.chronicle.wire.marshallable.NonStaticInnerClassWarningTest$Inner {\n" +
                "  hello: World\n" +
                "}\n", asText);
        Inner in2 = Marshallable.fromString(asText);
        try {
            expectException("Hello World (inner)");
            in2.helloHello();
            fail();
        } catch (NullPointerException e) {
            // expected
        }
    }

    void useOutClass() {
        Jvm.warn().on(getClass(), "Hello World (outer)");
    }

    class Inner extends SelfDescribingMarshallable {
        String hello;

        void helloHello() {
            Jvm.warn().on(getClass(), "Hello " + hello + " (inner)");
            useOutClass();
        }
    }
}
