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

import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.core.StringContains.containsString;

// A test class to ensure small double values are marshaled and unmarshaled correctly using
// Chronicle-Wire.
// See: https://github.com/OpenHFT/Chronicle-Wire/issues/240
public class SmallDoublesMarshallingTest extends WireTestCommon {

    // An example class containing a single double value to be marshaled and unmarshaled.
    public static class Example extends SelfDescribingMarshallable {
        private double doubleVal;

        public double doubleVal() {
            return doubleVal;
        }

        public Example doubleVal(double doubleVal) {
            this.doubleVal = doubleVal;
            return this;
        }
    }

    // A test to ensure that a specific small double value is marshaled and unmarshaled correctly.
    @Test
    public void marshallingTest() {
        final Example example = new Example().doubleVal(1.104326320059551E-14);
        final String textRepr = example.toString();
        final Example demarshalled = WireType.TEXT.fromString(Example.class, textRepr);

        MatcherAssert.assertThat(textRepr, containsString("1.104326320059551E-14"));
        Assert.assertEquals(example.doubleVal(), demarshalled.doubleVal(), 1e-14);
    }
}
