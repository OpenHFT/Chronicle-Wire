package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.WireType;
import org.junit.Ignore;
import org.junit.Test;

public class Issue277 {

    @Ignore("https://github.com/OpenHFT/Chronicle-Wire/issues/277")
    @Test(expected = IllegalArgumentException.class)
    public void reproduce() {
        // This is needed in order to reproduce the problem
        ClassAliasPool.CLASS_ALIASES.addAlias(Data1.class);

        // This should throw a RuntimeException as Data1.class != Data2.class
        Data2 o2 = WireType.TEXT.fromString(Data2.class, "!Data1 {\n" +
                "  name: Tom,\n" +
                "  age: 25,\n" +
                "  address: \"21 high street, Liverpool\"\n" +
                "}\n");

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
