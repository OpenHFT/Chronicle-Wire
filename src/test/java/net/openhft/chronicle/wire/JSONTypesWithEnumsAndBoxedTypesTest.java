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

import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

/**
 * relates to https://github.com/OpenHFT/Chronicle-Wire/issues/324
 */
@RunWith(value = Parameterized.class)
public class JSONTypesWithEnumsAndBoxedTypesTest extends net.openhft.chronicle.wire.WireTestCommon {

    private final boolean useTypes;

    @Parameterized.Parameters(name = "useTypes={0}")
    public static Collection<Object[]> wireTypes() {
        return Arrays.asList(
                new Object[]{true},
                new Object[]{false}
        );
    }

    public JSONTypesWithEnumsAndBoxedTypesTest(boolean useTypes) {
        this.useTypes = useTypes;
    }

    enum Location {
        PITS, TRACK, GRAVEL
    }

    static class F1 extends AbstractMarshallableCfg {

        private String surname;

        // change this to and int from an Integer and, it will work !
        private Integer car;
        private Location location;

        public F1(String surname, int car, Location location) {
            this.surname = surname;
            this.car = car;
            this.location = location;
        }
    }

    @Test
    public void test() {
        ClassAliasPool.CLASS_ALIASES.addAlias(F1.class);
        final JSONWire jsonWire = new JSONWire()
                .useTypes(useTypes);

        jsonWire.getValueOut()
                .object(new F1("Hamilton", 44, Location.TRACK));

         System.out.println(jsonWire.bytes());


        final String actual = jsonWire.getValueIn().object().toString();
        Assert.assertTrue(actual.contains("TRACK"));
    }
}
