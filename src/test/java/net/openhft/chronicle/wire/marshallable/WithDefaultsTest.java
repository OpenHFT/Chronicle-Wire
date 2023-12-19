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

import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.WireTestCommon;
import org.junit.Test;

import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;

public class WithDefaultsTest extends WireTestCommon {
    @Test
    public void writeMarshallable() {
        doTest(w -> {
        });
        doTest(w -> w.bytes.clear());
        doTest(w -> w.text = "bye");
        doTest(w -> w.flag = false);
        doTest(w -> w.num = 5);
    }

    void doTest(Consumer<WithDefaults> consumer) {
        WithDefaults wd = new WithDefaults();
        consumer.accept(wd);
        String cs = wd.toString();
        WithDefaults o = Marshallable.fromString(cs);
        assertEquals(cs, o.toString());
        assertEquals(wd, o);
    }
}
