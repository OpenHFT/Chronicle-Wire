/*
 * Copyright 2016 higherfrequencytrading.com
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

import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.wire.FieldInfo;
import net.openhft.chronicle.wire.Marshallable;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

/*
 * Created by Peter Lawrey on 09/05/16.
 */
@RunWith(value = Parameterized.class)
public class FieldInfoTest {
    private final Marshallable m;
    private final String fieldInfos;

    public FieldInfoTest(Marshallable m, String fieldInfos) {
        this.fieldInfos = fieldInfos;
        this.m = m;
    }

    @NotNull
    @Parameterized.Parameters
    public static Collection<Object[]> combinations() {
        @NotNull List<Object[]> list = new ArrayList<>();
        @NotNull Marshallable[] objects = {
                new Nested(new ScalarValues(), Collections.emptyList(), Collections.emptySet(), Collections.emptyMap(), "".split("")),
                new ScalarValues(1),
        };
        @NotNull String[] fields = {
                "[!FieldInfo {\n" +
                        "  name: values,\n" +
                        "  type: !type net.openhft.chronicle.wire.marshallable.ScalarValues,\n" +
                        "  bracketType: MAP,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.Nested\n" +
                        "}\n" +
                        ", !FieldInfo {\n" +
                        "  name: strings,\n" +
                        "  type: !type !seq,\n" +
                        "  bracketType: SEQ,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.Nested\n" +
                        "}\n" +
                        ", !FieldInfo {\n" +
                        "  name: ints,\n" +
                        "  type: !type !set,\n" +
                        "  bracketType: SEQ,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.Nested\n" +
                        "}\n" +
                        ", !FieldInfo {\n" +
                        "  name: map,\n" +
                        "  type: !type !map,\n" +
                        "  bracketType: MAP,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.Nested\n" +
                        "}\n" +
                        ", !FieldInfo {\n" +
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
                        ", !FieldInfo {\n" +
                        "  name: ch,\n" +
                        "  type: !type char,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !FieldInfo {\n" +
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
                        ", !FieldInfo {\n" +
                        "  name: l,\n" +
                        "  type: !type long,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !FieldInfo {\n" +
                        "  name: d,\n" +
                        "  type: !type double,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !FieldInfo {\n" +
                        "  name: flag2,\n" +
                        "  type: !type java.lang.Boolean,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !FieldInfo {\n" +
                        "  name: b2,\n" +
                        "  type: !type byte,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !FieldInfo {\n" +
                        "  name: s2,\n" +
                        "  type: !type short,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !FieldInfo {\n" +
                        "  name: ch2,\n" +
                        "  type: !type Char,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !FieldInfo {\n" +
                        "  name: i2,\n" +
                        "  type: !type int,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !FieldInfo {\n" +
                        "  name: f2,\n" +
                        "  type: !type Float32,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !FieldInfo {\n" +
                        "  name: l2,\n" +
                        "  type: !type long,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !FieldInfo {\n" +
                        "  name: d2,\n" +
                        "  type: !type Float64,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !FieldInfo {\n" +
                        "  name: aClass,\n" +
                        "  type: !type type,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !FieldInfo {\n" +
                        "  name: policy,\n" +
                        "  type: !type java.lang.annotation.RetentionPolicy,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !FieldInfo {\n" +
                        "  name: text,\n" +
                        "  type: !type String,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !FieldInfo {\n" +
                        "  name: date,\n" +
                        "  type: !type Date,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !FieldInfo {\n" +
                        "  name: time,\n" +
                        "  type: !type Time,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !FieldInfo {\n" +
                        "  name: dateTime,\n" +
                        "  type: !type DateTime,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !FieldInfo {\n" +
                        "  name: zonedDateTime,\n" +
                        "  type: !type ZonedDateTime,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !FieldInfo {\n" +
                        "  name: uuid,\n" +
                        "  type: !type java.util.UUID,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !FieldInfo {\n" +
                        "  name: bi,\n" +
                        "  type: !type java.math.BigInteger,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !FieldInfo {\n" +
                        "  name: bd,\n" +
                        "  type: !type java.math.BigDecimal,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        ", !FieldInfo {\n" +
                        "  name: file,\n" +
                        "  type: !type java.io.File,\n" +
                        "  bracketType: NONE,\n" +
                        "  parent: !type net.openhft.chronicle.wire.marshallable.ScalarValues\n" +
                        "}\n" +
                        "]",
        };
        for (int i = 0; i < objects.length; i++) {
            Marshallable object = objects[i];
            String fi = fields[i];
            @NotNull Object[] test = {object, fi};
            list.add(test);
        }
        return list;
    }

    @Test
    public void fieldInfo() {
        @NotNull List<FieldInfo> infos = m.$fieldInfos();
        assertEquals(fieldInfos, infos.toString());
    }

    @After
    public void checkRegisteredBytes() {
        BytesUtil.checkRegisteredBytes();
    }
}
