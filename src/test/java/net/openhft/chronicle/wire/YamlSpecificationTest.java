/*
 * Copyright 2016 higherfrequencytrading.com
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

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

/*
 * Created by Peter Lawrey on 25/08/15.
 */
@RunWith(Parameterized.class)
public class YamlSpecificationTest {
    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(String.class, "something");
        ClassAliasPool.CLASS_ALIASES.addAlias(Circle.class, "circle");
        ClassAliasPool.CLASS_ALIASES.addAlias(Shape.class, "shape");
        ClassAliasPool.CLASS_ALIASES.addAlias(Line.class, "line");
        ClassAliasPool.CLASS_ALIASES.addAlias(Label.class, "label");
    }

    private final String input;

    public YamlSpecificationTest(String input) {
        this.input = input;
    }

    @Parameterized.Parameters
    public static Collection tests() {
        return Arrays.asList(new String[][]{
                {"example2_1"},
                {"example2_2"},
                {"example2_3"},
//                {"example2_4"}, // TODO Fix map format
//                {"example2_5"}, // Not supported
//                {"example2_6"}, // TODO Fix map format
                {"example2_7"},// TODO Fix for multiple ---
//                {"example2_8"},// TODO Fix for multiple ---
                {"example2_9"},
//                {"example2_10"}, // TODO FIx handling of anchors
//                {"example2_11"}, // Not supported
//                {"example2_12"}, // Not supported
//                {"example2_13"}, // Not supported
//                {"example2_14"}, // Not supported
//                {"example2_15"}, // Not supported
//                {"example2_16"}, // Not supported
//                {"example2_17"}, // TODO Fix handling of double single quote.
//                {"example2_18"}, // Not supported
//                {"example2_19"}, // TODO fix handling of times.
//                {"example2_20"}, // TODO fix handling of times.
                {"example2_21"},
//                {"example2_22"}, // TODO fix handling of times.
//                {"example2_23"}, // Not supported
//                {"example2_24"}, // TODO FIx handling of anchors
//                {"example2_25"}, // TODO support set
//                {"example2_26"}, // TODO support omap
//                {"example2_27"}, // Not supported
//                {"example2_28"} // Not supported
        });
    }

    @Test
    public void decodeAs() throws IOException {
        @Nullable byte[] byteArr = getBytes(input + ".yaml");
        Bytes bytes = Bytes.wrapForRead(byteArr);
        @NotNull TextWire tw = new TextWire(bytes);
        @NotNull Bytes bytes2 = Bytes.allocateElasticDirect();
        @NotNull TextWire tw2 = new TextWire(bytes2);

        @Nullable Object o = tw.readObject();
        tw2.writeObject(o);
        @Nullable byte[] byteArr2 = getBytes(input + ".out.yaml");
        if (byteArr2 == null)
            byteArr2 = byteArr;
        String expected = Bytes.wrapForRead(byteArr2).toString().replace("\r\n", "\n");
        String actual = bytes2.toString();
        assertEquals(input, expected, actual);
    }

    @Nullable
    public byte[] getBytes(String file) throws IOException {
        InputStream is = getClass().getResourceAsStream("/specification/" + file);
        if (is == null) return null;
        int len = is.available();
        @NotNull byte[] byteArr = new byte[len];
        is.read(byteArr);
        return byteArr;
    }

    @After
    public void checkRegisteredBytes() {
        BytesUtil.checkRegisteredBytes();
    }
}
/*
--- !shape
  # Use the ! handle for presenting
  # tag:clarkevans.com,2002:circle
- !circle
  center: &ORIGIN {x: 73, y: 129}
  radius: 7
- !line
  start: *ORIGIN
  finish: { x: 89, y: 102 }
- !label
  start: *ORIGIN
  color: 0xFFEEBB
  text: Pretty vector drawing.
 */

class Shape implements Marshallable {
}

class Circle implements Marshallable {
}

class Line implements Marshallable {
}

class Label implements Marshallable {
}
