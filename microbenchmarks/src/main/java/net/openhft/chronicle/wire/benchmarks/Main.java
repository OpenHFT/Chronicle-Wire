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
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

enum Side {
    Buy, Sell;
}

enum DataFields implements WireKey {
    unknown, smallInt, longInt, price, flag, text, side;

    @Override
    public int code() {
        return ordinal();
    }
}

/**
 * Created by peter on 11/08/15.
 */
@State(Scope.Thread)
public class Main {
    final Bytes bytes = Bytes.allocateDirect(1024).unchecked(true);
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
                if (m.getAnnotation(Benchmark.class) != null)
                    m.invoke(main);
            }
        } else {
            Options opt = new OptionsBuilder()
                    .include(Main.class.getSimpleName())
                    .forks(1)
                    .mode(Mode.SampleTime)
                    .measurementIterations(100)
                    .timeUnit(TimeUnit.NANOSECONDS)
                    .build();

            new Runner(opt).run();
        }
    }

    @Benchmark
    public Bytes twireUTF() {
        return writeReadTest(twireUTF);
    }

    @Benchmark
    public Bytes twire8bit() {
        return writeReadTest(twire8bit);
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
    public Bytes rwire8bit() {
        return writeReadTest(rwire8bit);
    }

    @Benchmark
    public Bytes rwireUTF() {
        return writeReadTest(rwireUTF);
    }

    @NotNull
    public Bytes writeReadTest(Wire wire) {
        bytes.clear();
        wire.writeDocument(false, data);
        Wires.rawReadData(wire, data2);
        return bytes;
    }
}

class Data implements Marshallable {
    int smallInt = 0;
    long longInt = 0;
    double price = 0;
    boolean flag = false;
    StringBuilder text = new StringBuilder();
    Side side;
    private IntConsumer setSmallInt = x -> smallInt = x;
    private LongConsumer setLongInt = x -> longInt = x;
    private DoubleConsumer setPrice = x -> price = x;
    private BooleanConsumer setFlag = x -> flag = x;
    private Consumer<Side> setSide = x -> side = x;

    public Data(int smallInt, long longInt, double price, boolean flag, CharSequence text, Side side) {
        this.smallInt = smallInt;
        this.longInt = longInt;
        this.price = price;
        this.flag = flag;
        this.side = side;
        this.text.append(text);
    }

    public Data() {

    }

    @Override
    public void readMarshallable(WireIn wire) throws IllegalStateException {
        wire.read(DataFields.smallInt).int32(setSmallInt)
                .read(DataFields.longInt).int64(setLongInt)
                .read(DataFields.price).float64(setPrice)
                .read(DataFields.flag).bool(setFlag)
                .read(DataFields.text).text(text)
                .read(DataFields.side).asEnum(Side.class, setSide);
    }

    @Override
    public void writeMarshallable(WireOut wire) {
        wire.write(DataFields.smallInt).int32(smallInt)
                .write(DataFields.longInt).int64(longInt)
                .write(DataFields.price).float64(price)
                .write(DataFields.flag).bool(flag)
                .write(DataFields.text).text(text)
                .write(DataFields.side).asEnum(side);
    }
}