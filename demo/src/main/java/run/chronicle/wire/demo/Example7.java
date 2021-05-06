package run.chronicle.wire.demo;

import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.WireType;

import java.io.IOException;

public class Example7 {

    //Data1.class is in Alias pool but Data2.class is not.
    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(Data1.class);
    }

    public static void main(String[] args) throws IOException {

        Data1 data1 = new Data1();
        data1.name = "James";
        data1.age = 20;
        data1.address = "12 Kingston, London";
        Data2 o2 = WireType.TEXT.fromFile(Data2.class, "cfg1.yaml");
        System.out.println("o2 = " + o2);
        
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
        System.out.println(data1.toString());


        //Prints out:
        // !run.chronicle.wire.demo.Example7$Data2
        // name: james,
        // age: 20,
        // address: "12 Kingston, London"
        // }
        System.out.println(data2.toString());

        Data1 o1 = WireType.TEXT.fromString("!Data1 {\n" +
                "  name: Tom,\n" +
                "  age: 25,\n" +
                "  address: \"21 high street, Liverpool\"\n" +
                "}");
        System.out.println("o1 = " + o1);



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
