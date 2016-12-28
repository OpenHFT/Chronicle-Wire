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
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

/**
 * @author Rob Austin.
 */
public class BinaryToTextTest {

    @Test
    public void test() {
        Bytes tbytes = Bytes.elasticByteBuffer();
        @NotNull Wire tw = new BinaryWire(tbytes);
        tw.writeDocument(false, w->w.write(() -> "key").text("hello"));
        System.out.println(Wires.fromSizePrefixedBlobs(tbytes));
    }
}
