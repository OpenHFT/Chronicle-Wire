/*
 *     Copyright (C) 2015-2020 chronicle.software
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

    private static final BytesStore CHAR1 = BytesStoreFrom("A");
    private static final BytesStore CHAR2 = BytesStoreFrom("A2");
    private static final BytesStore CHAR4 = BytesStoreFrom("A234");
    private static final BytesStore CHAR8 = BytesStoreFrom("A2345678");
    private static final BytesStore CHAR16 = BytesStoreFrom("A234567890123456");
    private static final BytesStore CHAR32 = BytesStoreFrom("A2345678901234567890123456789012");
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
    protected static String newStringUTF8(BytesStore bs) {
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
        BytesStore bs = CHAR1;
        return newStringHiByte0(bs);
    }

    protected String newStringHiByte0(BytesStore bs) {
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

