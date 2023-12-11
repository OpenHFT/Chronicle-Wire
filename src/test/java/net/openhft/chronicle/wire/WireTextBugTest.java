/*
 * Copyright 2016-2020 chronicle.software
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.openhft.chronicle.wire;/*
 * Copyright 2016-2020 chronicle.software
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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.junit.Assert.assertEquals;

/**
 * @author Rob Austin
 */
public class WireTextBugTest extends WireTestCommon {

    @org.junit.Test
    public void testText() {
        ClassAliasPool.CLASS_ALIASES.addAlias(Bug.class);
        @NotNull Wire encodeWire = new BinaryWire(Bytes.elasticByteBuffer(), false, true, false, Integer.MAX_VALUE, "lzw", true);
        @NotNull Bug b = new Bug();
        b.setClOrdID("FIX.4.4:12345678_client1->FOO/MINI1-1234567891234-12");
        assertEquals("!Bug {\n" +
                "  clOrdID: \"FIX.4.4:12345678_client1->FOO/MINI1-1234567891234-12\"\n" +
                "}\n", b.toString());
        encodeWire.getValueOut().object(b);
        byte[] bytes = encodeWire.bytes().toByteArray();

        @NotNull Wire decodeWire = new BinaryWire(Bytes.wrapForRead(bytes));
        @Nullable Object o = decodeWire.getValueIn()
                .object(Object.class);
        @Nullable Bug b2 = (Bug) o;
        assertEquals("!Bug {\n" +
                "  clOrdID: \"FIX.4.4:12345678_client1->FOO/MINI1-1234567891234-12\"\n" +
                "}\n", b2.toString());

        encodeWire.bytes().releaseLast();
        decodeWire.bytes().releaseLast();
    }

    static class Bug extends SelfDescribingMarshallable {
        private String clOrdID;

        public String getClOrdID() {
            return clOrdID;
        }

        public void setClOrdID(String aClOrdID) {
            clOrdID = aClOrdID;
        }
    }
}
