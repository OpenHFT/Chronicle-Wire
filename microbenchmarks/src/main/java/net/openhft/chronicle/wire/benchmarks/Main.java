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

package net.openhft.chronicle.wire.benchmarks;

import net.openhft.affinity.Affinity;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.*;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

enum DataFields implements WireKey {
    unknown, smallInt, longInt, price, flag, text, side;

    @Override
    public int code() {
        return ordinal();
    }
}

@Retention(RetentionPolicy.RUNTIME)
@interface PrintAsText {
}

/**
 * Created by peter on 11/08/15.
 */
@State(Scope.Thread)
public class Main {
    final Bytes bytes = Bytes.allocateDirect(128).unchecked(true);
    final Wire twireUTF = new TextWire(bytes, false);
    final Wire twire8bit = new TextWire(bytes, true);
    final Wire bwireFFF = new BinaryWire(bytes, false, false, false);
    final Wire bwireTFF = new BinaryWire(bytes, true, false, false);
    final Wire bwireFTF = new BinaryWire(bytes, false, true, false);
    final Wire bwireTTF = new BinaryWire(bytes, true, true, false);
    final Wire bwireFTT = new BinaryWire(bytes, false, true, true);
    final Wire rwireUTF = new RawWire(bytes, false);
    final Wire rwire8bit = new RawWire(bytes, true);

    final Data data = new Data(123, 1234567890L, 1234, true, "Hello World", Side.Sell);
    final Data data2 = new Data();

    public static void main(String... args) throws RunnerException, InvocationTargetException, IllegalAccessException {
        Affinity.setAffinity(2);
        if (Jvm.isDebug()) {
            Main main = new Main();
            for (Method m : Main.class.getMethods()) {
                if (m.getAnnotation(Benchmark.class) != null) {
                    m.invoke(main);
                    main.bytes.readPosition(0);
                    System.out.println("Test " + m.getName() + " used " + main.bytes.readRemaining() + " bytes.");
                    System.out.println(m.getAnnotation(PrintAsText.class) != null
                            ? Wires.fromSizePrefixedBinaryToText(main.bytes) : main.bytes.toHexString());

                }
            }
        } else {
            int time = Boolean.getBoolean("longTest") ? 30 : 2;
            System.out.println("measurementTime: " + time + " secs");
            Options opt = new OptionsBuilder()
                    .include(Main.class.getSimpleName())
                    .forks(1)
                    .mode(Mode.SampleTime)
                    .measurementTime(TimeValue.seconds(time))
                    .timeUnit(TimeUnit.NANOSECONDS)
                    .build();

            new Runner(opt).run();
        }
    }
/*
    @Benchmark
    @PrintAsText
    public Data twireUTF() {
        return writeReadTest(twireUTF);
    }

    @Benchmark
    @PrintAsText
    public Data twire8bit() {
        return writeReadTest(twire8bit);
    }

    @Benchmark
    @PrintAsText
    public Data bwireFFF() {
        return writeReadTest(bwireFFF);
    }

    @Benchmark
    @PrintAsText
    public Data bwireFTF() {
        return writeReadTest(bwireFTF);
    }

    @Benchmark
    @PrintAsText
    public Data bwireFTT() {
        return writeReadTest(bwireFTT);
    }

    @Benchmark
    @PrintAsText
    public Data bwireTFF() {
        return writeReadTest(bwireTFF);
    }

    @Benchmark
    @PrintAsText
    public Data bwireTTF() {
        return writeReadTest(bwireTTF);
    }

    @Benchmark
    public Data rwire8bit() {
        return writeReadTest(rwire8bit);
    }

    @Benchmark
    public Data rwireUTF() {
        return writeReadTest(rwireUTF);
    }

    @NotNull
    public Data writeReadTest(Wire wire) {
        bytes.clear();
        wire.writeDocument(false, data);
        Wires.rawReadData(wire, data2);
        return data2;
    }*/

    /*
    Test bytesMarshallable used 42 bytes.
00000000 26 00 00 00 00 00 00 00  00 48 93 40 59 0B 48 65 &······· ·H·@Y·He
00000010 6C 6C 6F 20 57 6F 72 6C  64 04 53 65 6C 6C 7B 00 llo Worl d·Sell{·
00000020 00 00 D2 02 96 49 00 00  00 00                   ·····I·· ··
     */
    @Benchmark
    public Data bytesMarshallable() {
        bytes.clear();
        bytes.writeSkip(4);
        data.writeMarshallable(bytes);
        // write the actual length.
        bytes.writeInt(0, (int) (bytes.writePosition() - 4));

        int len = bytes.readInt();
        data2.readMarshallable(bytes);
        return data2;
    }
}

