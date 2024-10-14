/*
 * Copyright 2016-2020 chronicle.software
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
import org.jetbrains.annotations.NotNull;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.StreamCorruptedException;
import java.util.Arrays;
import java.util.Collection;

import static net.openhft.chronicle.bytes.Bytes.allocateElasticOnHeap;

@Ignore("Long running test")
@RunWith(value = Parameterized.class)
public class BinaryWirePerfTest extends WireTestCommon {

    // Define test parameters
    final int testId;
    final boolean fixed;
    final boolean numericField;
    final boolean fieldLess;
    @NotNull
    Bytes<?> bytes = allocateElasticOnHeap();

    // Constructor for parameterized test
    public BinaryWirePerfTest(int testId, boolean fixed, boolean numericField, boolean fieldLess) {
        this.testId = testId;
        this.fixed = fixed;
        this.numericField = numericField;
        this.fieldLess = fieldLess;
    }

    // Provide combinations of parameters for the test
    @Parameterized.Parameters
    public static Collection<Object[]> combinations() {
        return Arrays.asList(
                new Object[]{0, false, false, false},
                new Object[]{1, true, false, false},
                new Object[]{2, false, true, false},
                new Object[]{3, true, true, false},
                new Object[]{4, false, false, true},
                new Object[]{5, true, false, true}
        );
    }

    // Create and return a Wire object based on the test parameters
    @NotNull
    private Wire createBytes() {
        bytes.clear();
        @NotNull Wire wire = testId == -1
                ? new RawWire(bytes)
                : new BinaryWire(bytes, fixed, numericField, fieldLess, Integer.MAX_VALUE, "lzw");

        return wire;
    }

    // *************************************************************************
    // Test Cases
    // *************************************************************************

    // Performance test for Wire serialization and deserialization
    @Test
    public void wirePerf() throws StreamCorruptedException {
       // System.out.println("Custom TestId: " + testId + ", fixed: " + fixed + ", numberField: " + numericField + ", fieldLess: " + fieldLess);
        @NotNull Wire wire = createBytes();

        // Custom type serialization and deserialization test
        @NotNull MyTypesCustom a = new MyTypesCustom();
        for (int t = 0; t < 3; t++) {
            a.text.setLength(0);
            a.text.append("Hello World");
            wirePerf0(wire, a, new MyTypesCustom(), t);
        }

       // System.out.println("Reflective TestId: " + testId + ", fixed: " + fixed + ", numberField: " + numericField + ", fieldLess: " + fieldLess);
        @NotNull MyTypes b = new MyTypes();
        for (int t = 0; t < 3; t++) {
            b.text.setLength(0);
            b.text.append("Hello World");
            wirePerf0(wire, b, new MyTypes(), t);
        }
    }

    private void wirePerf0(@NotNull Wire wire, @NotNull MyTypes a, @NotNull MyTypes b, int t) {
        long start = System.nanoTime();
        int runs = 200000;
        for (int i = 0; i < runs; i++) {
            wire.clear();
            a.flag = (i & 1) != 0;
            a.d = i;
            a.i = i;
            a.l = i;
            a.writeMarshallable(wire);

            b.readMarshallable(wire);
        }
        //long rate = (System.nanoTime() - start) / runs;

       // System.out.printf("(vars) %,d : %,d ns avg, len= %,d%n", t, rate, wire.bytes().readPosition());
    }

    // Performance test for serializing and deserializing integers with Wire
    @Test
    public void wirePerfInts() {
       // System.out.println("TestId: " + testId + ", fixed: " + fixed + ", numberField: " + numericField + ", fieldLess: " + fieldLess);
        @NotNull Wire wire = createBytes();
        @NotNull MyType2 a = new MyType2();
        for (int t = 0; t < 3; t++) {
            wirePerf0(wire, a, new MyType2(), t);
        }
    }

    // Common method to test serialization and deserialization for type MyType2
    private void wirePerf0(@NotNull Wire wire, @NotNull MyType2 a, @NotNull MyType2 b, int t) {
        long start = System.nanoTime();
        int runs = 300000;
        for (int i = 0; i < runs; i++) {
            wire.clear();
            a.i = i;
            a.l = i;
            a.writeMarshallable(wire);

            b.readMarshallable(wire);
        }
        long rate = (System.nanoTime() - start) / runs;
       // System.out.printf("(ints) %,d : %,d ns avg, len= %,d%n", t, rate, wire.bytes().readPosition());
    }

    // *************************************************************************
    // Internal Classes
    // *************************************************************************

    // MyType2 class for serialization and deserialization tests
    static class MyType2 implements Marshallable {
        int i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x;

        // Write this object's fields to the provided Wire
        @Override
        public void writeMarshallable(@NotNull WireOut wire) {
            wire.write(Fields.I).int32(i)
                    .write(Fields.J).int32(j)
                    .write(Fields.K).int32(k)
                    .write(Fields.L).int32(l)
                    .write(Fields.M).int32(m)
                    .write(Fields.N).int32(n)
                    .write(Fields.O).int32(o)
                    .write(Fields.P).int32(p)

                    .write(Fields.Q).int32(q)
                    .write(Fields.R).int32(r)
                    .write(Fields.S).int32(s)
                    .write(Fields.T).int32(t)
                    .write(Fields.U).int32(u)
                    .write(Fields.V).int32(v)
                    .write(Fields.W).int32(w)
                    .write(Fields.X).int32(v);
        }

        // Read this object's fields from the provided Wire
        @Override
        public void readMarshallable(@NotNull WireIn wire) {
            wire.read(Fields.I).int32(this, (o, x) -> o.i = x)
                    .read(Fields.J).int32(this, (o, x) -> o.j = x)
                    .read(Fields.K).int32(this, (o, x) -> o.k = x)
                    .read(Fields.L).int32(this, (o, x) -> o.l = x)
                    .read(Fields.M).int32(this, (o, x) -> o.m = x)
                    .read(Fields.N).int32(this, (o, x) -> o.n = x)
                    .read(Fields.O).int32(this, (t, x) -> t.o = x)
                    .read(Fields.P).int32(this, (o, x) -> o.p = x)
                    .read(Fields.Q).int32(this, (o, x) -> o.q = x)
                    .read(Fields.R).int32(this, (o, x) -> o.r = x)
                    .read(Fields.S).int32(this, (o, x) -> o.s = x)
                    .read(Fields.T).int32(this, (o, x) -> o.t = x)
                    .read(Fields.U).int32(this, (o, x) -> o.u = x)
                    .read(Fields.V).int32(this, (o, x) -> o.v = x)
                    .read(Fields.W).int32(this, (o, x) -> o.w = x)
                    .read(Fields.X).int32(this, (o, x) -> o.x = x);
        }

        // Enum to represent the field keys for serialization/deserialization
        enum Fields implements WireKey {
            I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X;

            // Return the code for this field key (ordinal value)
            @Override
            public int code() {
                return ordinal();
            }
        }
    }
}
