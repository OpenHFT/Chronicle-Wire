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
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BinaryToTextTest extends WireTestCommon {

    @Test
    public void test() {
        @SuppressWarnings("rawtypes")
        Bytes<?> tbytes = Bytes.allocateElasticOnHeap();
        @NotNull Wire tw = new BinaryWire(tbytes);
        tw.usePadding(true);
        tw.writeDocument(false, w -> w.write(() -> "key").text("hello"));
        assertEquals("" +
                        "--- !!data #binary\n" +
                        "key: hello\n",
                Wires.fromSizePrefixedBlobs(tw));
    }
}
