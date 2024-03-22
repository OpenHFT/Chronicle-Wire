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
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FloatDtoTest extends WireTestCommon {

    @Test
    public void test() {
        @NotNull final Value value = new Value(99, 2000f);
        final Bytes<?> bytes = Bytes.elasticByteBuffer();
        final Wire w = WireType.BINARY.apply(bytes);
        w.write().marshallable(value);
        @NotNull Value object1 = new Value(0, 0.0f);
        w.read().marshallable(object1);
        assertEquals(value, object1);
        bytes.releaseLast();
    }

    static class Key extends SelfDescribingMarshallable implements KeyedMarshallable {
        @SuppressWarnings("unused")
        int uiid;

        Key(int uiid) {
            this.uiid = uiid;
        }
    }

    static class Value extends Key implements Marshallable {

        @SuppressWarnings("unused")
        final float myFloat;

        Value(int uiid,
              float myFloat) {
            super(uiid);
            this.myFloat = myFloat;
        }
    }
}
