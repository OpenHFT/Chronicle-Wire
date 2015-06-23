/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import java.io.Serializable;
import java.util.function.Function;

import static org.junit.Assert.*;

/**
 * Created by peter on 23/06/15.
 */
public class WireSerializedLambdaTest {
    @Test
    public void testIsLambda() {
        Function<String, String> fun = (Function<String, String> & Serializable) String::toUpperCase;
        assertTrue(WireSerializedLambda.isSerializableLambda(fun.getClass()));
        int a = 5;
        Function<Integer, Integer> fun2 = (Function<Integer, Integer> & Serializable) i -> i + a;
        assertTrue(WireSerializedLambda.isSerializableLambda(fun2.getClass()));
        assertFalse(WireSerializedLambda.isSerializableLambda(this.getClass()));
    }

    @Test
    public void testTextWire() {
        Wire wire = new TextWire(Bytes.elasticByteBuffer());
        Function<String, String> fun = (Function<String, String> & Serializable) String::toUpperCase;
        wire.write().object(fun);

        assertEquals("\"\": !SerializedLambda {\n" +
                "  cc: !type net.openhft.chronicle.wire.WireSerializedLambdaTest,\n" +
                "  fic: java/util/function/Function,\n" +
                "  fimn: apply,\n" +
                "  fims: (Ljava/lang/Object;)Ljava/lang/Object;,\n" +
                "  imk: 5,\n" +
                "  ic: java/lang/String,\n" +
                "  imn: toUpperCase,\n" +
                "  ims: ()Ljava/lang/String;,\n" +
                "  imt: (Ljava/lang/String;)Ljava/lang/String;,\n" +
                "  ca: [  ]\n" +
                "}\n", wire.bytes().toString());

        Function function = wire.read().object(Function.class);
        assertEquals("HELLO", function.apply("hello"));
    }

    @Test
    public void testBinaryWire() {
        Wire wire = new BinaryWire(Bytes.elasticByteBuffer());
        Function<String, String> fun = (Function<String, String> & Serializable) String::toUpperCase;
        wire.write().object(fun);

        assertEquals("00000000 C0 B6 10 53 65 72 69 61  6C 69 7A 65 64 4C 61 6D ···Seria lizedLam\n" +
                "00000010 62 64 61 82 06 01 00 00  C2 63 63 BC 33 6E 65 74 bda····· ·cc·3net\n" +
                "00000020 2E 6F 70 65 6E 68 66 74  2E 63 68 72 6F 6E 69 63 .openhft .chronic\n" +
                "00000030 6C 65 2E 77 69 72 65 2E  57 69 72 65 53 65 72 69 le.wire. WireSeri\n" +
                "00000040 61 6C 69 7A 65 64 4C 61  6D 62 64 61 54 65 73 74 alizedLa mbdaTest\n" +
                "00000050 C3 66 69 63 FB 6A 61 76  61 2F 75 74 69 6C 2F 66 ·fic·jav a/util/f\n" +
                "00000060 75 6E 63 74 69 6F 6E 2F  46 75 6E 63 74 69 6F 6E unction/ Function\n" +
                "00000070 C4 66 69 6D 6E E5 61 70  70 6C 79 C4 66 69 6D 73 ·fimn·ap ply·fims\n" +
                "00000080 B8 26 28 4C 6A 61 76 61  2F 6C 61 6E 67 2F 4F 62 ·&(Ljava /lang/Ob\n" +
                "00000090 6A 65 63 74 3B 29 4C 6A  61 76 61 2F 6C 61 6E 67 ject;)Lj ava/lang\n" +
                "000000a0 2F 4F 62 6A 65 63 74 3B  C3 69 6D 6B 05 C2 69 63 /Object; ·imk··ic\n" +
                "000000b0 F0 6A 61 76 61 2F 6C 61  6E 67 2F 53 74 72 69 6E ·java/la ng/Strin\n" +
                "000000c0 67 C3 69 6D 6E EB 74 6F  55 70 70 65 72 43 61 73 g·imn·to UpperCas\n" +
                "000000d0 65 C3 69 6D 73 F4 28 29  4C 6A 61 76 61 2F 6C 61 e·ims·() Ljava/la\n" +
                "000000e0 6E 67 2F 53 74 72 69 6E  67 3B C3 69 6D 74 B8 26 ng/Strin g;·imt·&\n" +
                "000000f0 28 4C 6A 61 76 61 2F 6C  61 6E 67 2F 53 74 72 69 (Ljava/l ang/Stri\n" +
                "00000100 6E 67 3B 29 4C 6A 61 76  61 2F 6C 61 6E 67 2F 53 ng;)Ljav a/lang/S\n" +
                "00000110 74 72 69 6E 67 3B C2 63  61 82 00 00 00 00       tring;·c a·····  \n", wire.bytes().toHexString());

        Function function = wire.read().object(Function.class);
        assertEquals("HELLO", function.apply("hello"));
    }

}