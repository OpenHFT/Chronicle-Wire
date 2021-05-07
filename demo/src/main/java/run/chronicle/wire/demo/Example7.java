package run.chronicle.wire.demo;

import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.WireType;

import java.io.IOException;


//This example shows how to use alias names for classes
public class Example7 {

    //Data1.class is added to Alias pool but Data2.class is not
    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(Data1.class);
    }

    public static void main(String[] args) throws IOException {

        Data1 data1 = new Data1();
        data1.name = "James";
        data1.age = 20;
        data1.address = "12 Kingston, London";

        Data2 data2 = new Data2();
        data2.name = "James";
        data2.age = 20;
        data2.address = "12 Kingston, London";

        // Prints out:
        //!Data1 {
        //  name: james,
        //  age: 20,
        //  address: "12 Kingston, London"
        //}
        System.out.println(data1);

        //Prints out:
        // !run.chronicle.wire.demo.Example7$Data2
        // name: james,
        // age: 20,
        // address: "12 Kingston, London"
        // }
        System.out.println(data2);

// Reading from yaml files.
// For Data1 object, alias name is used in the yaml file (cfg1.yaml). Data2 object should be loaded from a yaml file
// with the complete name of class (including package name) otherwise you will receive an Exception. See cfg1.yaml and cfg2.yaml

        Data1 o1 = WireType.TEXT.fromFile("cfg1.yaml");

        // Prints out:
        // o1 = !Data1 {
        // name: Tom,
        // age: 25,
        // address: "21 High street, Liverpool"
        // }
        System.out.println("o1 = " + o1);

        Data2 o2 = WireType.TEXT.fromFile("cfg2.yaml");

        // Prints out:
        // o2 = !run.chronicle.wire.demo.Example7$Data2 {
        // name: Helen,
        // age: 19,
        // address: "15 Royal Way, Liverpool"
        // }
        System.out.println("o2 = " + o2);
    }

    private static class Data1 extends SelfDescribingMarshallable {
        String name;
        int age;
        String address;
    }

    private static class Data2 extends SelfDescribingMarshallable {
        String name;
        int age;
        String address;
    }
}
