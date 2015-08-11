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
import net.openhft.chronicle.wire.util.BooleanConsumer;
import org.jetbrains.annotations.NotNull;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

/**
 * Created by peter on 11/08/15.
 */
@State(Scope.Thread)
public class Main {
    final Bytes bytes = Bytes.allocateDirect(1024).unchecked(true);
    final Wire twire = new TextWire(bytes);
    final Wire bwireFFF = new BinaryWire(bytes, false, false, false);
    final Wire bwireTFF = new BinaryWire(bytes, false, false, false);
    final Wire bwireFTF = new BinaryWire(bytes, false, true, false);
    final Wire bwireTTF = new BinaryWire(bytes, false, true, false);
    final Wire bwireFTT = new BinaryWire(bytes, false, true, true);
    final Wire rwire = new RawWire(bytes);
    int i = 0;
    long j = 0;
    double d = 0;
    boolean b = false;
    StringBuilder sb = new StringBuilder();
    IntConsumer xi = x -> i = x;

    /*
        @Benchmark
        public Bytes readDocument() {
            bytes.clear();
            wire.writeDocument(false, w -> w.write(Fields.hello).int32(123));
            wire.readDocument(null, w -> w.read(Fields.hello).int32(intConsumer));
            return bytes;
        }

        @Benchmark
        public Bytes rawReadData() {
            bytes.clear();
            wire.writeDocument(false, w -> w.write(Fields.hello).int32(123));
            Wires.rawReadData(wire, w -> w.read(Fields.hello).int32(intConsumer));
            return bytes;
        }
    */
    LongConsumer xj = x -> j = x;
    DoubleConsumer xd = x -> d = x;
    BooleanConsumer xb = x -> b = x;
    ReadMarshallable wireInConsumer = w -> w
            .read(Fields.one).int32(xi)
            .read(Fields.two).int64(xj)
            .read(Fields.three).text(sb)
            .read(Fields.four).float64(xd)
            .read(Fields.five).bool(xb);

    public static void main(String... args) throws RunnerException, InvocationTargetException, IllegalAccessException {
        Affinity.setAffinity(2);
        if (Jvm.isDebug()) {
            Main main = new Main();
            for (Method m : Main.class.getMethods()) {
                if (m.getAnnotation(Benchmark.class) != null)
                    m.invoke(main);
            }
        } else {
            Options opt = new OptionsBuilder()
                    .include(Main.class.getSimpleName())
                    .forks(3)
                    .mode(Mode.SampleTime)
                    .timeUnit(TimeUnit.NANOSECONDS)
                    .build();

            new Runner(opt).run();
        }
    }

    @Benchmark
    public Bytes twire() {
        return writeReadTest(twire);
    }

    @Benchmark
    public Bytes bwireFFF() {
        return writeReadTest(bwireFFF);
    }

    @Benchmark
    public Bytes bwireFTF() {
        return writeReadTest(bwireFTF);
    }

    @Benchmark
    public Bytes bwireFTT() {
        return writeReadTest(bwireFTT);
    }

    @Benchmark
    public Bytes bwireTFF() {
        return writeReadTest(bwireTFF);
    }

    @Benchmark
    public Bytes bwireTTF() {
        return writeReadTest(bwireTTF);
    }

    @Benchmark
    public Bytes rwire() {
        return writeReadTest(rwire);
    }

    @NotNull
    public Bytes writeReadTest(Wire wire) {
        bytes.clear();
        wire.writeDocument(false, w -> w
                .write(Fields.one).int32(123)
                .write(Fields.two).int64(1234567890L)
                .write(Fields.three).text("Hello World")
                .write(Fields.four).float64(1e6)
                .write(Fields.five).bool(true));
        Wires.rawReadData(wire, wireInConsumer);
        return bytes;
    }

    enum Fields implements WireKey {
        unknown,
        one, two, three, four, five;

        @Override
        public int code() {
            return ordinal();
        }
    }
}
