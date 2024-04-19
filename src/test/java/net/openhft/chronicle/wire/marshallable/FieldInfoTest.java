/*
 * Copyright 2016-2020 chronicle.software
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.wire.FieldInfo;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.WireTestCommon;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
// Runner to enable parameterized tests for the FieldInfoTest class
@RunWith(value = Parameterized.class)
public class FieldInfoTest extends WireTestCommon {

    // Marshallable object for test scenarios
    private final Marshallable m;

    // Expected field information as string
    private final String fieldInfos;

    // Constructor initializes the Marshallable object and expected field information
    public FieldInfoTest(Marshallable m, String fieldInfos) {
        this.fieldInfos = fieldInfos;
        this.m = m;
    }

    // Provide test data combinations for the parameterized test
    @NotNull
    @Parameterized.Parameters
    public static Collection<Object[]> combinations() {

        // Collection to store different test combinations
        @NotNull List<Object[]> list = new ArrayList<>();

        // Sample Marshallable objects for the test scenarios
        @NotNull Marshallable[] objects = {
                new Nested(new ScalarValues(), Collections.emptyList(), Collections.emptySet(), Collections.emptyMap(), "".split("")),
                new ScalarValues(1),
        };

        // Corresponding field information for the above objects
        @NotNull String[] fields = {
                "[!net.openhft.chronicle.wire.internal.fieldinfo.ObjectFieldInfo {\n" +
                        "  name: values,\n" +
                        "  type: !type net.openhft.chronicle.wire.marshallable.ScalarValues,\n" +
                        "  bracketType: MAP,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.Nested\n" +
                        "}\n" +
                        ", !net.openhft.chronicle.wire.internal.fieldinfo.ObjectFieldInfo {\n" +
                        "  name: strings,\n" +
                        "  type: !type !seq,\n" +
                        "  bracketType: SEQ,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.Nested\n" +
                        "}\n" +
                        ", !net.openhft.chronicle.wire.internal.fieldinfo.ObjectFieldInfo {\n" +
                        "  name: ints,\n" +
                        "  type: !type !set,\n" +
                        "  bracketType: SEQ,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.Nested\n" +
                        "}\n" +
                        ", !net.openhft.chronicle.wire.internal.fieldinfo.ObjectFieldInfo {\n" +
                        "  name: map,\n" +
                        "  type: !type !map,\n" +
                        "  bracketType: MAP,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.Nested\n" +
                        "}\n" +
                        ", !net.openhft.chronicle.wire.internal.fieldinfo.ObjectFieldInfo {\n" +
                        "  name: array,\n" +
                        "  type: !type String[],\n" +
                        "  bracketType: SEQ,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.Nested\n" +
                        "}\n" +
                        "]",
                "[!FieldInfo {\n" +
                        "  name: flag,\n" +
                        "  type: !type boolean,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !FieldInfo {\n" +
                        "  name: b,\n" +
                        "  type: !type byte,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !FieldInfo {\n" +
                        "  name: s,\n" +
                        "  type: !type short,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !net.openhft.chronicle.wire.internal.fieldinfo.CharFieldInfo {\n" +
                        "  name: ch,\n" +
                        "  type: !type char,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !net.openhft.chronicle.wire.internal.fieldinfo.IntFieldInfo {\n" +
                        "  name: i,\n" +
                        "  type: !type int,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !FieldInfo {\n" +
                        "  name: f,\n" +
                        "  type: !type float,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !net.openhft.chronicle.wire.internal.fieldinfo.LongFieldInfo {\n" +
                        "  name: l,\n" +
                        "  type: !type long,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !net.openhft.chronicle.wire.internal.fieldinfo.DoubleFieldInfo {\n" +
                        "  name: d,\n" +
                        "  type: !type double,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !net.openhft.chronicle.wire.internal.fieldinfo.ObjectFieldInfo {\n" +
                        "  name: flag2,\n" +
                        "  type: !type java.lang.Boolean,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !net.openhft.chronicle.wire.internal.fieldinfo.ObjectFieldInfo {\n" +
                        "  name: b2,\n" +
                        "  type: !type byte,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !net.openhft.chronicle.wire.internal.fieldinfo.ObjectFieldInfo {\n" +
                        "  name: s2,\n" +
                        "  type: !type short,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !net.openhft.chronicle.wire.internal.fieldinfo.ObjectFieldInfo {\n" +
                        "  name: ch2,\n" +
                        "  type: !type Char,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !net.openhft.chronicle.wire.internal.fieldinfo.ObjectFieldInfo {\n" +
                        "  name: i2,\n" +
                        "  type: !type int,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !net.openhft.chronicle.wire.internal.fieldinfo.ObjectFieldInfo {\n" +
                        "  name: f2,\n" +
                        "  type: !type Float32,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !net.openhft.chronicle.wire.internal.fieldinfo.ObjectFieldInfo {\n" +
                        "  name: l2,\n" +
                        "  type: !type long,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !net.openhft.chronicle.wire.internal.fieldinfo.ObjectFieldInfo {\n" +
                        "  name: d2,\n" +
                        "  type: !type Float64,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !net.openhft.chronicle.wire.internal.fieldinfo.ObjectFieldInfo {\n" +
                        "  name: aClass,\n" +
                        "  type: !type type,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !net.openhft.chronicle.wire.internal.fieldinfo.ObjectFieldInfo {\n" +
                        "  name: policy,\n" +
                        "  type: !type java.lang.annotation.RetentionPolicy,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !net.openhft.chronicle.wire.internal.fieldinfo.ObjectFieldInfo {\n" +
                        "  name: text,\n" +
                        "  type: !type String,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !net.openhft.chronicle.wire.internal.fieldinfo.ObjectFieldInfo {\n" +
                        "  name: date,\n" +
                        "  type: !type Date,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !net.openhft.chronicle.wire.internal.fieldinfo.ObjectFieldInfo {\n" +
                        "  name: time,\n" +
                        "  type: !type Time,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !net.openhft.chronicle.wire.internal.fieldinfo.ObjectFieldInfo {\n" +
                        "  name: dateTime,\n" +
                        "  type: !type DateTime,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !net.openhft.chronicle.wire.internal.fieldinfo.ObjectFieldInfo {\n" +
                        "  name: zonedDateTime,\n" +
                        "  type: !type ZonedDateTime,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !net.openhft.chronicle.wire.internal.fieldinfo.ObjectFieldInfo {\n" +
                        "  name: uuid,\n" +
                        "  type: !type UUID,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !net.openhft.chronicle.wire.internal.fieldinfo.ObjectFieldInfo {\n" +
                        "  name: bi,\n" +
                        "  type: !type java.math.BigInteger,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !net.openhft.chronicle.wire.internal.fieldinfo.ObjectFieldInfo {\n" +
                        "  name: bd,\n" +
                        "  type: !type java.math.BigDecimal,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !net.openhft.chronicle.wire.internal.fieldinfo.ObjectFieldInfo {\n" +
                        "  name: file,\n" +
                        "  type: !type java.io.File,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !net.openhft.chronicle.wire.internal.fieldinfo.ObjectFieldInfo {\n" +
                        "  name: dynamicEnum,\n" +
                        "  type: !type net.openhft.chronicle.wire.marshallable.TestDynamicEnum,\n" +
                        "  bracketType: UNKNOWN,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        "]",
        };

        // Populate the test combinations list
        for (int i = 0; i < objects.length; i++) {
            Marshallable object = objects[i];
            String fi = fields[i];
            @NotNull Object[] test = {object, fi};
            list.add(test);
        }

        return list;
    }

    // Test method to ensure the field information from the Marshallable object matches the expected value
    @Test
    public void fieldInfo() {
        @NotNull List<FieldInfo> infos = m.$fieldInfos();
        assertEquals(fieldInfos, infos.toString());
    }
}
