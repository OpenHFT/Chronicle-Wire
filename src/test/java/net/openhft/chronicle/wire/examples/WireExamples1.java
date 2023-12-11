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

package net.openhft.chronicle.wire.examples;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;
import net.openhft.chronicle.wire.YamlWire;

public class WireExamples1 {

    public static void main(String[] args) {
        example1();
        example2();
    }

    public static void example1() {

        // allows the the YAML to refer to car, rather than net.openhft.chronicle.wire.WireExamples$Car
        ClassAliasPool.CLASS_ALIASES.addAlias(Car.class);

        Wire wire = new YamlWire(Bytes.allocateElasticOnHeap());
        wire.getValueOut().object(new Car("Lewis Hamilton", 44));
        System.out.println(wire);
    }

    public static void example2() {

        ClassAliasPool.CLASS_ALIASES.addAlias(Car.class);

        Wire wire = WireType.FIELDLESS_BINARY.apply(Bytes.allocateElasticOnHeap());
        wire.getValueOut().object(new Car("Lewis Hamilton", 44));
        System.out.println(wire.bytes().toHexString());
    }

    public static class Car implements Marshallable {
        private int number;
        private String driver;

        public Car(String driver, int number) {
            this.driver = driver;
            this.number = number;
        }
    }

}
