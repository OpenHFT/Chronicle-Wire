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
