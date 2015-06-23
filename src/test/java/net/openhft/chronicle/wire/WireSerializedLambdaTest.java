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
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.util.SerializableFunction;
import org.junit.Test;

import java.io.Serializable;
import java.util.function.Function;

import static org.junit.Assert.*;

/**
 * Created by peter on 23/06/15.
 */
public class WireSerializedLambdaTest {
    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(Fun.class);
    }
    @Test
    public void testIsLambda() {
        Function<String, String> fun = (Function<String, String> & Serializable) String::toUpperCase;
        assertTrue(WireSerializedLambda.isSerializableLambda(fun.getClass()));
        int a = 5;
        SerializableFunction<Integer, Integer> fun2 = i -> i + a;
        assertTrue(WireSerializedLambda.isSerializableLambda(fun2.getClass()));
        assertFalse(WireSerializedLambda.isSerializableLambda(this.getClass()));
    }

    @Test
    public void testTextWire() {
        Wire wire = new TextWire(Bytes.elasticByteBuffer());
        SerializableFunction<String, String> fun = String::toUpperCase;
        wire.write(() -> "one").object(fun)
                .write(() -> "two").object(Fun.ADD_A);


        assertEquals("one: !SerializedLambda {\n" +
                "  cc: !type net.openhft.chronicle.wire.WireSerializedLambdaTest,\n" +
                "  fic: net/openhft/chronicle/core/util/SerializableFunction,\n" +
                "  fimn: apply,\n" +
                "  fims: (Ljava/lang/Object;)Ljava/lang/Object;,\n" +
                "  imk: 5,\n" +
                "  ic: java/lang/String,\n" +
                "  imn: toUpperCase,\n" +
                "  ims: ()Ljava/lang/String;,\n" +
                "  imt: (Ljava/lang/String;)Ljava/lang/String;,\n" +
                "  ca: [  ]\n" +
                "}\n" +
                "two: !Fun ADD_A\n", wire.bytes().toString());

        Function function = wire.read(() -> "one").object(Function.class);
        assertEquals("HELLO", function.apply("hello"));

        Function function2 = wire.read(() -> "two").object(Function.class);
        assertEquals("helloA", function2.apply("hello"));
    }

    @Test
    public void testBinaryWire() {
        Wire wire = new BinaryWire(Bytes.elasticByteBuffer());
        SerializableFunction<String, String> fun = String::toUpperCase;
        wire.write(() -> "one").object(fun)
                .write(() -> "two").object(Fun.ADD_A);

        assertEquals("[pos: 0, rlim: 330, wlim: 8EiB, cap: 8EiB ] " +
                "Ãone¶⒗SerializedLambda\\u0082 ⒈٠٠" +
                "Âcc¼3net.openhft.chronicle.wire.WireSerializedLambdaTest" +
                "Ãfic¸4net/openhft/chronicle/core/util/SerializableFunction" +
                "Äfimnåapply" +
                "Äfims¸&(Ljava/lang/Object;)Ljava/lang/Object;" +
                "Ãimk⒌" +
                "Âicðjava/lang/String" +
                "ÃimnëtoUpperCase" +
                "Ãimsô()Ljava/lang/String;" +
                "Ãimt¸&(Ljava/lang/String;)Ljava/lang/String;" +
                "Âca\\u0082٠٠٠٠" +
                "Ãtwo¶⒊FunåADD_A", wire.bytes().toDebugString());

        Function function = wire.read().object(Function.class);
        assertEquals("HELLO", function.apply("hello"));

        Function function2 = wire.read(() -> "two").object(Function.class);
        assertEquals("helloA", function2.apply("hello"));
    }
}
