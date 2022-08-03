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

package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.*;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;

public class MarshallableWithOverwriteFalseTest extends WireTestCommon {

    @Test
    public void test() {

        MyDto2 myDto2 = new MyDto2();
        MyDto myDto1 = new MyDto();

        myDto2.myDto.put("", myDto1);
        myDto1.strings.add("hello");
        myDto1.strings.add("world");

        String cs = myDto2.toString();
       // System.out.println(cs);
        MyDto2 o = (MyDto2) Marshallable.fromString(cs);

        assertEquals(2, o.myDto.get("").strings.size());
    }

    static class MyDto extends SelfDescribingMarshallable {
        List<String> strings = new ArrayList<>();

        public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {

            // WORKS
             // Wires.readMarshallable(this, wire, true);  // WORKS

            // FAILS
            Wires.readMarshallable(this, wire, false);
        }
    }

    static class MyDto2 extends SelfDescribingMarshallable {
        Map<String, MyDto> myDto = new TreeMap<String, MyDto>();
    }
}
