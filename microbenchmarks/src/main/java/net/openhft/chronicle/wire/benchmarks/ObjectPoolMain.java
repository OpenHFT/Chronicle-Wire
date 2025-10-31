/*
 * Copyright 2016-2025 chronicle.software
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
package net.openhft.chronicle.wire.benchmarks;

import net.openhft.affinity.Affinity;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.util.Bit8StringInterner;
import net.openhft.chronicle.core.Jvm;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
public class ObjectPoolMain {

    private static final BytesStore<?, ?> CHAR1 = BytesStoreFrom("A");
    private static final BytesStore<?, ?> CHAR2 = BytesStoreFrom("A2");
    private static final BytesStore<?, ?> CHAR4 = BytesStoreFrom("A234");
    private static final BytesStore<?, ?> CHAR8 = BytesStoreFrom("A2345678");
    private static final BytesStore<?, ?> CHAR16 = BytesStoreFrom("A234567890123456");
    private static final BytesStore<?, ?> CHAR32 = BytesStoreFrom("A2345678901234567890123456789012");
    private static final byte[] BUFFER = new byte[32];
    private final Bit8StringInterner si = new Bit8StringInterner(64);

    private static BytesStore<?, Void> BytesStoreFrom(String s) {
        return BytesStore.nativeStoreFrom(s.getBytes(StandardCharsets.ISO_8859_1));
    }

    public static void main(String... args) throws RunnerException, InvocationTargetException, IllegalAccessException {
        Affinity.setAffinity(2);
        if (Jvm.isDebug()) {
            ObjectPoolMain main = new ObjectPoolMain();
            for (Method m : ObjectPoolMain.class.getMethods()) {
                if (m.getAnnotation(Benchmark.class) != null) {
                    for (int i = 0; i < 5; i++)
                        m.invoke(main);
                }
            }
        } else {
            int time = Jvm.getBoolean("longTest") ? 30 : 2;
            System.out.println("measurementTime: " + time + " secs");
            Options opt = new OptionsBuilder()
                    .include(ObjectPoolMain.class.getSimpleName())
                    .warmupIterations(5)
                    .measurementIterations(5)
                    .forks(1)
                    .mode(Mode.AverageTime)
                    .measurementTime(TimeValue.seconds(time))
                    .timeUnit(TimeUnit.MICROSECONDS)
                    .build();

            new Runner(opt).run();
        }
    }

//    @NotNull
    protected static String newStringUTF8(BytesStore<?, ?> bs) {
        int length = bs.length();
        bs.read(0, BUFFER, 0, length);
        return new String(BUFFER, 0, length, StandardCharsets.UTF_8);
    }

    /*
        @Benchmark
        public String char01() {
            return si.intern(CHAR1);
        }

        @Benchmark
        public String char02() {
            return si.intern(CHAR2);
        }

        @Benchmark
        public String char04() {
            return si.intern(CHAR4);
        }

        @Benchmark
        public String char08() {
            return si.intern(CHAR8);
        }

        @Benchmark
        public String char16() {
            return si.intern(CHAR16);
        }

        @Benchmark
        public String char32() {
            return si.intern(CHAR32);
        }
    */
//    @Benchmark
    public String newString01() {
        return newStringUTF8(CHAR1);
    }

//    @Benchmark
    public String newString02() {
        return newStringUTF8(CHAR2);
    }

//    @Benchmark
    public String newString04() {
        return newStringUTF8(CHAR4);
    }

//    @Benchmark
    public String newString08() {
        return newStringUTF8(CHAR8);
    }

//    @Benchmark
    public String newString16() {
        return newStringUTF8(CHAR16);
    }

//    @Benchmark
    public String newString32() {
        return newStringUTF8(CHAR32);
    }

//    @Benchmark
    public String newStringB01() {
        BytesStore<?, ?> bs = CHAR1;
        return newStringHiByte0(bs);
    }

    protected String newStringHiByte0(BytesStore<?, ?> bs) {
        int length = bs.length();
        bs.read(0, BUFFER, 0, length);
        return new String(BUFFER, 0, 0, length);
    }

//    @Benchmark
    public String newStringB02() {
        return newStringHiByte0(CHAR2);
    }

//    @Benchmark
    public String newStringB04() {
        return newStringHiByte0(CHAR4);
    }

//    @Benchmark
    public String newStringB08() {
        return newStringHiByte0(CHAR8);
    }

//    @Benchmark
    public String newStringB16() {
        return newStringHiByte0(CHAR16);
    }

//    @Benchmark
    public String newStringB32() {
        return newStringHiByte0(CHAR32);
    }
}
