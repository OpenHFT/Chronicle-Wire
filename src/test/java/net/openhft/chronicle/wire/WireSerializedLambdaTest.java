/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
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

@SuppressWarnings("unchecked")
public class WireSerializedLambdaTest extends WireTestCommon {
    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(Fun.class);
        ClassAliasPool.CLASS_ALIASES.addAlias(Update.class);
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
                .write(() -> "three").object(Update.INCR);

       // System.out.println(wire.bytes().toString());

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
                "two: !Fun ADD_A\n" +
                "three: !Update INCR\n", wire.bytes().toString());

        @Nullable Function<String, String> function = wire.read(() -> "one").object(Function.class);
        assertEquals("HELLO", function.apply("hello"));

        @Nullable Function<String, String> function2 = wire.read(() -> "two").object(Function.class);
        assertEquals("helloA", function2.apply("hello"));

        @Nullable Consumer<AtomicLong> updater = wire.read(() -> "three").object(Consumer.class);
        @NotNull AtomicLong aLong = new AtomicLong();
        updater.accept(aLong);
        assertEquals(1, aLong.get());

        wire.bytes().releaseLast();
    }

    @Test
    public void testBinaryWire() {
        @NotNull Wire wire = new BinaryWire(new HexDumpBytes());
        assert wire.startUse();
        SerializableFunction<String, String> fun = String::toUpperCase;
        wire.write(() -> "one").object(fun)
                .write(() -> "two").object(Fun.ADD_A)
                .write(() -> "three").object(Update.DECR);

        assertEquals("c3 6f 6e 65                                     # one:\n" +
                        "b6 10 53 65 72 69 61 6c 69 7a 65 64 4c 61 6d 62 # SerializedLambda\n" +
                        "64 61 82 21 01 00 00                            # WireSerializedLambda$$Lambda$\n" +
                        "c2 63 63                                        # cc:\n" +
                        "bc 33 6e 65 74 2e 6f 70 65 6e 68 66 74 2e 63 68 # net.openhft.chronicle.wire.WireSerializedLambdaTest\n" +
                        "72 6f 6e 69 63 6c 65 2e 77 69 72 65 2e 57 69 72\n" +
                        "65 53 65 72 69 61 6c 69 7a 65 64 4c 61 6d 62 64\n" +
                        "61 54 65 73 74 c3 66 69 63                      # fic:\n" +
                        "b8 34 6e 65 74 2f 6f 70 65 6e 68 66 74 2f 63 68 # net/openhft/chronicle/core/util/SerializableFunction\n" +
                        "72 6f 6e 69 63 6c 65 2f 63 6f 72 65 2f 75 74 69\n" +
                        "6c 2f 53 65 72 69 61 6c 69 7a 61 62 6c 65 46 75\n" +
                        "6e 63 74 69 6f 6e c4 66 69 6d 6e                # fimn:\n" +
                        "e5 61 70 70 6c 79                               # apply\n" +
                        "c4 66 69 6d 73                                  # fims:\n" +
                        "b8 26 28 4c 6a 61 76 61 2f 6c 61 6e 67 2f 4f 62 # (Ljava/lang/Object;)Ljava/lang/Object;\n" +
                        "6a 65 63 74 3b 29 4c 6a 61 76 61 2f 6c 61 6e 67\n" +
                        "2f 4f 62 6a 65 63 74 3b c3 69 6d 6b             # imk:\n" +
                        "a1 05                                           # 5\n" +
                        "c2 69 63                                        # ic:\n" +
                        "f0 6a 61 76 61 2f 6c 61 6e 67 2f 53 74 72 69 6e # java/lang/String\n" +
                        "67 c3 69 6d 6e                                  # imn:\n" +
                        "eb 74 6f 55 70 70 65 72 43 61 73 65             # toUpperCase\n" +
                        "c3 69 6d 73                                     # ims:\n" +
                        "f4 28 29 4c 6a 61 76 61 2f 6c 61 6e 67 2f 53 74 # ()Ljava/lang/String;\n" +
                        "72 69 6e 67 3b c3 69 6d 74                      # imt:\n" +
                        "b8 26 28 4c 6a 61 76 61 2f 6c 61 6e 67 2f 53 74 # (Ljava/lang/String;)Ljava/lang/String;\n" +
                        "72 69 6e 67 3b 29 4c 6a 61 76 61 2f 6c 61 6e 67\n" +
                        "2f 53 74 72 69 6e 67 3b c2 63 61                # ca:\n" +
                        "82 00 00 00 00                                  # sequence\n" +
                        "c3 74 77 6f                                     # two:\n" +
                        "b6 03 46 75 6e                                  # Fun\n" +
                        "e5 41 44 44 5f 41                               # ADD_A\n" +
                        "c5 74 68 72 65 65                               # three:\n" +
                        "b6 06 55 70 64 61 74 65                         # Update\n" +
                        "e4 44 45 43 52                                  # DECR\n",
                wire.bytes().toHexString().replaceAll("\\$\\$Lambda.*", "\\$\\$Lambda\\$"));

        @Nullable Function<String, String> function = wire.read().object(Function.class);
        assertEquals("HELLO", function.apply("hello"));

        @Nullable Function<String, String> function2 = wire.read(() -> "two").object(Function.class);
        assertEquals("helloA", function2.apply("hello"));

        @Nullable Consumer<AtomicLong> updater = wire.read(() -> "three").object(Consumer.class);
        @NotNull AtomicLong aLong = new AtomicLong();
        updater.accept(aLong);
        assertEquals(-1, aLong.get());

        wire.bytes().releaseLast();
    }
}
