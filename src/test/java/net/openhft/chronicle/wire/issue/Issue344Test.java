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

package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.Wires;
import org.junit.Assert;
import org.junit.Test;

public class Issue344Test extends WireTestCommon {
    @Test
    public void testFFFF() {
        runWith('\uFFFF');
    }

    @Test
    public void testFFFE() {
        runWith('\uFFFE');
    }

    private void runWith(char test) {
        final TestData data = new TestData();
        data.testChar = test;
        final TestData copyData = new TestData();
        Wires.copyTo(data, copyData);
        Assert.assertEquals(data.testChar, copyData.testChar);
    }

    private static class TestData implements Marshallable {
        public char testChar;
    }
}
