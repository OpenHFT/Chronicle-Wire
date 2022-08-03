/*
 * Copyright 2016-2022 chronicle.software
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
import net.openhft.chronicle.bytes.HexDumpBytes;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.Serializable;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assume.assumeFalse;

@RunWith(value = Parameterized.class)
public class Issue341Test extends WireTestCommon {

    private final WireType wireType;

    public Issue341Test(WireType wireType) {
        this.wireType = wireType;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                new Object[]{WireType.BINARY},
                new Object[]{WireType.BINARY_LIGHT},
                new Object[]{WireType.JSON},
                new Object[]{WireType.JSON_ONLY},
                new Object[]{WireType.TEXT},
                new Object[]{WireType.YAML},
                new Object[]{WireType.YAML_ONLY},
        });
    }

    @Test
    public void instant() {
        final MyClass source = new MyClass();
        source.instant = Instant.ofEpochMilli(1_000_000_000_000L);

        final Bytes<?> bytes = new HexDumpBytes();
        final Wire wire = wireType.apply(bytes);

        wire.getValueOut().object((Class) source.getClass(), source);
        System.out.println(wireType + "\n"
                + (wire.getValueOut().isBinary() ? bytes.toHexString() : bytes.toString()));

        final MyClass target = wire.getValueIn().object(source.getClass());
        Assert.assertEquals(source, target);

    }


    @Test
    public void testComparableSerializable() {
        // for backward compatibility, this doesn't support types
        assumeFalse(wireType == WireType.JSON);
        final MyComparableSerializable source = new MyComparableSerializable("hello");

        final Bytes<?> bytes = new HexDumpBytes();
        final Wire wire = wireType.apply(bytes);

        wire.getValueOut().object((Class) source.getClass(), source);
        System.out.println(wireType + "\n"
                + (wire.getValueOut().isBinary() ? bytes.toHexString() : bytes.toString()));

        final MyComparableSerializable target = wire.getValueIn().object(source.getClass());
        Assert.assertEquals(source.value, target.value);
    }

    static final class MyClass extends SelfDescribingMarshallable {
        Instant instant;
    }

    static final class MyComparableSerializable implements Serializable, Comparable<MyComparableSerializable> {
        final String value;

        MyComparableSerializable(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }

        @Override
        public int compareTo(@NotNull MyComparableSerializable o) {
            return value.compareTo(o.value);
        }
    }
}
