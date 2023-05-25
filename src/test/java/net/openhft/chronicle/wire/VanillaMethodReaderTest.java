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
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.marshallable.TriviallyCopyableMarketData;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

public class VanillaMethodReaderTest {

    public interface MyMethod {
        void msg(String str);
    }

    @Test
    public void testPredicateFalse() {

        Bytes b = Bytes.elasticByteBuffer();
        Wire w = new TextWire(b);
        MyMethod build1 = w.methodWriterBuilder(MyMethod.class)
                .build();
        build1.msg("hi");

        final String[] value = new String[1];
        MethodReader reader = new VanillaMethodReaderBuilder(w)
                .predicate(x -> false)
                .build((MyMethod) str -> value[0] = str);

        Assert.assertFalse(reader.readOne());
        Assert.assertNull(value[0]);
    }

    @Test
    public void testPredicateTrue() {

        Bytes b = Bytes.elasticByteBuffer();
        Wire w = new TextWire(b);
        MyMethod build1 = w.methodWriterBuilder(MyMethod.class)
                .build();

        build1.msg("hi");

        VanillaMethodReaderBuilder builder = new VanillaMethodReaderBuilder(w);
        builder.predicate(x -> true);

        final String[] value = new String[1];
        MethodReader reader = builder.build((MyMethod) str -> value[0] = str);

        Assert.assertTrue(reader.readOne());
        Assert.assertEquals("hi", value[0]);
    }

    @Test
    public void logMessage0() {
        // layout is different
        assumeFalse(Jvm.isAzulZing());
        TriviallyCopyableMarketData data = new TriviallyCopyableMarketData();
        data.securityId(0x828282828282L);

        Wire wire = WireType.BINARY_LIGHT.apply(new HexDumpBytes());
        wire.methodWriter(ITCO.class).marketData(data);
        assertEquals("" +
                        "9e 00 00 00                                     # msg-length\n" +
                        "b9 0a 6d 61 72 6b 65 74 44 61 74 61             # marketData: (event)\n" +
                        "80 90 82 82 82 82 82 82 00 00 00 00 00 00 00 00 # TriviallyCopyableMarketData\n" +
                        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
                        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
                        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
                        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
                        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
                        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
                        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
                        "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00\n" +
                        "00 00\n",
                wire.bytes().toHexString());
        try (DocumentContext dc = wire.readingDocument()) {
            final ValueIn marketData = dc.wire().read("marketData");
            assertEquals("" +
                            "read md - 00000010 80 90 82 82 82 82 82 82  00 00 00 00 00 00 00 00 ········ ········\n" +
                            "00000020 00 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00 ········ ········\n" +
                            "........\n" +
                            "000000a0 00 00                                            ··               ",
                    VanillaMethodReader.logMessage0("md", marketData));
        }
    }

    interface ITCO {
        void marketData(TriviallyCopyableMarketData tcmd);
    }

}