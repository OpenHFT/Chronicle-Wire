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

package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.Mocker;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import org.junit.Test;

import java.io.StringWriter;

import static org.junit.Assert.*;

interface WithDefault {
    void method1(String text);

    default void method2(String text2) {
        throw new UnsupportedOperationException();
    }
}

public class DefaultMethodHandlingTest extends WireTestCommon {
    private static void doTest(WireType wireType) {
        Wire wire = wireType.apply(Bytes.allocateElasticOnHeap());
        WithDefault withDefault = wire.methodWriter(WithDefault.class);
        withDefault.method1("one");
        withDefault.method2("two");
        assertEquals("method1: one\n" +
                "...\n" +
                "method2: two\n" +
                "...\n", wire.toString());

        StringWriter sw = new StringWriter();
        MethodReader reader = wire.methodReader(Mocker.logging(WithDefault.class, "", sw));
        assertTrue(reader.readOne());
        assertTrue(reader.readOne());
        assertFalse(reader.readOne());
        assertEquals("method1[one]\n" +
                "method2[two]\n", sw.toString().replace("\r", ""));
    }

    @Test
    public void withDefault() {
        doTest(WireType.TEXT);
    }

    @Test
    public void withDefaultYaml() {
        doTest(WireType.YAML_ONLY);
    }
}
