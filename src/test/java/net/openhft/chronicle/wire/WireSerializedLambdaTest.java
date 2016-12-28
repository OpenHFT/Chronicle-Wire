/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.util.SerializableFunction;
import net.openhft.chronicle.core.util.SerializableUpdater;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.Assert.*;

/**
 * Created by peter on 23/06/15.
 */
public class WireSerializedLambdaTest {
    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(Fun.class);
        ClassAliasPool.CLASS_ALIASES.addAlias(Updat.class);
    }
    @Test
    public void testIsLambda() {
        @NotNull Function<String, String> fun = (Function<String, String> & Serializable) String::toUpperCase;
        assertTrue(WireSerializedLambda.isSerializableLambda(fun.getClass()));
        int a = 5;
        @NotNull SerializableFunction<Integer, Integer> fun2 = i -> i + a;
        assertTrue(WireSerializedLambda.isSerializableLambda(fun2.getClass()));
        SerializableUpdater<AtomicInteger> upd = AtomicInteger::incrementAndGet;
        assertTrue(WireSerializedLambda.isSerializableLambda(upd.getClass()));
        assertFalse(WireSerializedLambda.isSerializableLambda(this.getClass()));
    }

    @Test
    public void testTextWire() {
        @NotNull Wire wire = new TextWire(Bytes.elasticByteBuffer());
        SerializableFunction<String, String> fun = String::toUpperCase;

        wire.write(() -> "one").object(fun)
                .write(() -> "two").object(Fun.ADD_A)
                .write(() -> "three").object(Updat.INCR);

         System.out.println(wire.bytes().toString());

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
                "  ca: [\n" +
                "  ]\n" +
                "}\n" +
                "two: !Fun ADD_A\n" +
                "three: !Updat INCR\n", wire.bytes().toString());

        @Nullable Function<String, String> function = wire.read(() -> "one").object(Function.class);
        assertEquals("HELLO", function.apply("hello"));

        @Nullable Function<String, String> function2 = wire.read(() -> "two").object(Function.class);
        assertEquals("helloA", function2.apply("hello"));

        @Nullable Consumer<AtomicLong> updater = wire.read(() -> "three").object(Consumer.class);
        @NotNull AtomicLong aLong = new AtomicLong();
        updater.accept(aLong);
        assertEquals(1, aLong.get());
    }

    @Test
    public void testBinaryWire() {
        @NotNull Wire wire = new BinaryWire(Bytes.elasticByteBuffer());
        assert wire.startUse();
        SerializableFunction<String, String> fun = String::toUpperCase;
        wire.write(() -> "one").object(fun)
                .write(() -> "two").object(Fun.ADD_A)
                .write(() -> "three").object(Updat.DECR);

        assertEquals("[pos: 0, rlim: 348, wlim: 8EiB, cap: 8EiB ] ‖" +
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
                "Ãtwo¶⒊FunåADD_A" +
                "Åthree¶⒌UpdatäDECR‡", wire.bytes().toDebugString(348));

        @Nullable Function<String, String> function = wire.read().object(Function.class);
        assertEquals("HELLO", function.apply("hello"));

        @Nullable Function<String, String> function2 = wire.read(() -> "two").object(Function.class);
        assertEquals("helloA", function2.apply("hello"));

        @Nullable Consumer<AtomicLong> updater = wire.read(() -> "three").object(Consumer.class);
        @NotNull AtomicLong aLong = new AtomicLong();
        updater.accept(aLong);
        assertEquals(-1, aLong.get());
    }
}
