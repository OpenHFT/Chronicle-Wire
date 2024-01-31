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
import net.openhft.chronicle.core.util.Mocker;
import net.openhft.chronicle.core.util.ObjectUtils;
import net.openhft.chronicle.wire.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.StringWriter;
import java.lang.reflect.Proxy;

import static org.junit.Assert.*;

public class MethodWriterByInterfaceTest extends WireTestCommon {
    @Before
    public void setup() {
        ObjectUtils.defaultObjectForInterface(c -> Class.forName(c.getName() + "mpl"));
    }

    @After
    public void teardown() {
        ObjectUtils.defaultObjectForInterface(c -> c);
    }

    @Test
    public void writeReadViaImplementation() {
        checkWriteReadViaImplementation(WireType.TEXT, false);
    }

    @Test
    public void writeReadViaImplementationGenerateTuples() {
        checkWriteReadViaImplementation(WireType.TEXT, true);
    }

    @Test
    public void writeReadViaImplementationYaml() {
        checkWriteReadViaImplementation(WireType.YAML_ONLY, false);
    }

    private void checkWriteReadViaImplementation(WireType wireType, boolean generateTuples) {
        Wire tw = wireType.apply(Bytes.allocateElasticOnHeap());
        tw.generateTuples(generateTuples);
        MWBI0 mwbi0 = tw.methodWriter(MWBI0.class);
        mwbi0.method(new MWBImpl("name", 1234567890123456L));
        assertFalse(Proxy.isProxyClass(mwbi0.getClass()));
        assertEquals("method: {\n" +
                "  name: name,\n" +
                "  time: 2009-02-13T23:31:30.123456\n" +
                "}\n" +
                "...\n", tw.toString());
        StringWriter sw = new StringWriter();
        MethodReader reader = tw.methodReader(Mocker.logging(MWBI0.class, "", sw));
        assertFalse(Proxy.isProxyClass(reader.getClass()));
        assertTrue(reader.readOne());
        assertEquals("method[!net.openhft.chronicle.wire.method.MethodWriterByInterfaceTest$MWBImpl {\n" +
                "  name: name,\n" +
                "  time: 2009-02-13T23:31:30.123456\n" +
                "}\n" +
                "]\n", sw.toString().replace("\r", ""));
    }

    interface MWBI {
        String name();

        long time();
    }

    interface MWBI0 {
        void method(MWBI mwbi);
    }

    static class MWBImpl extends SelfDescribingMarshallable implements MWBI {
        String name;
        @LongConversion(MicroTimestampLongConverter.class)
        long time;

        MWBImpl(String name, long time) {
            this.name = name;
            this.time = time;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public long time() {
            return time;
        }
    }
}
