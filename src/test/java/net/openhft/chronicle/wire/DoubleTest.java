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
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.bytes.internal.UnsafeText;
import org.junit.Assert;
import org.junit.Test;

import static net.openhft.chronicle.core.pool.ClassAliasPool.CLASS_ALIASES;
import static net.openhft.chronicle.wire.Marshallable.fromString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

// see also UnsafeTextBytesTest
public class DoubleTest extends WireTestCommon {

    static class TwoDoubleDto extends SelfDescribingMarshallable {
        double price;
        double qty;
    }

    /**
     * relates to https://github.com/OpenHFT/Chronicle-Wire/issues/299 Fixed case where a serializable 'double' value sometimes has trailing zero
     */
    @Test
    public void testParsingForTwoDoubles() {
        CLASS_ALIASES.addAlias(TwoDoubleDto.class);
        final String EXPECTED = "!TwoDoubleDto {\n" +
                "  price: 43298.21,\n" +
                "  qty: 0.2886\n" +
                "}\n";
        final TwoDoubleDto twoDoubleDto = fromString(TwoDoubleDto.class, EXPECTED);

        Assert.assertEquals(EXPECTED, twoDoubleDto.toString());
    }

    @Test
    public void testManyDoubles() {
        final Bytes<?> bytes = Bytes.elasticByteBuffer();
        final long address = bytes.clear().addressForRead(0);

        for (double aDouble = -1; aDouble < 1; aDouble += 0.00001) {
            bytes.clear();
            aDouble = Maths.round6(aDouble);
            final long end = UnsafeText.appendDouble(address, aDouble);
            bytes.readLimit(end - address);
            double d2 = bytes.parseDouble();
            assertEquals(aDouble, d2, Math.ulp(aDouble));
            final String message = bytes.toString();
            assertFalse(message + " has trailing 0", message.endsWith("0"));
        }
        bytes.releaseLast();
    }
}
