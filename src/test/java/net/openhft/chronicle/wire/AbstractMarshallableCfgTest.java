package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

public class AbstractMarshallableCfgTest extends WireTestCommon{
    static class MyAMC extends AbstractMarshallableCfg {
        NestedAMC nestedAMC = new NestedAMC();
        NestedSDM nestedSDM = new NestedSDM();
    }

    static class NestedAMC extends AbstractMarshallableCfg {
        long number = 128;
        boolean flag;
    }

    static class NestedSDM extends SelfDescribingMarshallable {
        Bytes bytes = Bytes.elasticHeapByteBuffer();
        double amt = 1.0;
    }

    @Test
    public void asString() {
        MyAMC myAMC = new MyAMC();
        assertEquals("" +
                        "!net.openhft.chronicle.wire.AbstractMarshallableCfgTest$MyAMC {\n" +
                        "}\n",
                myAMC.toString());

        myAMC.nestedAMC.number = 0;
        myAMC.nestedAMC.flag = true;
        myAMC.nestedSDM.bytes.append("Hi");
        assertEquals("" +
                        "!net.openhft.chronicle.wire.AbstractMarshallableCfgTest$MyAMC {\n" +
                        "  nestedAMC: {\n" +
                        "    number: 0,\n" +
                        "    flag: true\n" +
                        "  },\n" +
                        "  nestedSDM: {\n" +
                        "    bytes: Hi,\n" +
                        "    amt: 1.0\n" +
                        "  }\n" +
                        "}\n",
                myAMC.toString());
    }

    @Test
    public void deepCopy() {
        MyAMC myAMC = new MyAMC();
        MyAMC myAMC2 = myAMC.deepCopy();
        assertNotSame(myAMC.nestedAMC, myAMC2.nestedAMC);
        assertNotSame(myAMC.nestedSDM, myAMC2.nestedSDM);
        assertNotSame(myAMC.nestedSDM.bytes, myAMC2.nestedSDM.bytes);
    }
}
