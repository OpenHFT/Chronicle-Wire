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

import org.junit.Assert;
import org.junit.Test;

import java.time.ZoneId;

public class TestMarshallableZoneId {

    public static class MySelfDescribingMarshallable extends SelfDescribingMarshallable {
        ZoneId zoneId;
    }

    @Test
    public void testMySelfDescribingMarshallable() {

        final MySelfDescribingMarshallable expected = new MySelfDescribingMarshallable();
        expected.zoneId = ZoneId.of("UTC");

        JSONWire jsonWire = new JSONWire().useTypes(true);
        jsonWire.getValueOut().object(expected);

        final MySelfDescribingMarshallable actual = jsonWire.getValueIn().object(MySelfDescribingMarshallable.class);
        Assert.assertEquals(expected, actual);
    }

    public static class MyAbstractMarshallableCfg extends AbstractMarshallableCfg {
        ZoneId zoneId;
    }

    @Test
    public void testMyAbstractMarshallableCfg() {

        final MyAbstractMarshallableCfg expected = new MyAbstractMarshallableCfg();
        expected.zoneId = ZoneId.of("UTC");

        JSONWire jsonWire = new JSONWire().useTypes(true);
        jsonWire.getValueOut().object(expected);

        final MyAbstractMarshallableCfg actual = jsonWire.getValueIn().object(MyAbstractMarshallableCfg.class);
        Assert.assertEquals(expected, actual);
    }

}
