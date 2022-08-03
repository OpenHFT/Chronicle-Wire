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
import org.junit.Assert;
import org.junit.Test;

public class JSONNanTest extends WireTestCommon {

    @Test
    public void writeNaN() {
        Bytes<?> b = Bytes.elasticByteBuffer();
        try {
            Wire wire = WireType.JSON.apply(b);
            Dto value = new Dto();
            value.value = Double.NaN;
            wire.write().marshallable(value);
            Assert.assertEquals("\"\":{\"value\":null}", wire.toString());
        } finally {
            b.releaseLast();
        }
    }

    /**
     * JSON spec says that NAN should be written as null
     */
    @Test
    public void readNan() {
        Bytes<?> b = Bytes.from("\"\":{\"value\":null}");
        Wire wire = WireType.JSON.apply(b);
        Dto value = wire.read().object(Dto.class);
        Assert.assertTrue(Double.isNaN(value.value));
    }

    /**
     * JSON spec says that NAN should be written as null
     */
    @Test
    public void readNanWithSpaceAteEnd() {
        Bytes<?> b = Bytes.from("\"\":{\"value\":null }");
        Wire wire = WireType.JSON.apply(b);
        Dto value = wire.read().object(Dto.class);
        Assert.assertTrue(Double.isNaN(value.value));
    }

    /**
     * JSON spec says that NAN should be written as null
     */
    @Test
    public void readNanWithSpaceAtStart() {
        Bytes<?> b = Bytes.from("\"\":{\"value\": null}");
        Wire wire = WireType.JSON.apply(b);
        Dto value = wire.read().object(Dto.class);
        Assert.assertTrue(Double.isNaN(value.value));
    }

public static class Dto extends SelfDescribingMarshallable {
    double value;
}
}
