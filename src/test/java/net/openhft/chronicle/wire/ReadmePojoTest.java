package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by peter on 17/03/16.
 */
public class ReadmePojoTest {
    static {
        ClassAliasPool.CLASS_ALIASES.addAlias(MyPojos.class);
    }

    @Test
    public void testFromString() throws IOException {
        MyPojos mps = new MyPojos("test-list");
        mps.myPojos.add(new MyPojo("text1", 1, 1.1));
        mps.myPojos.add(new MyPojo("text2", 2, 2.2));

        System.out.println(mps);
        MyPojos mps2 = Marshallable.fromString(mps.toString());
        assertEquals(mps, mps2);

        String text = "!MyPojos {\n" +
                "  name: test-list,\n" +
                "  myPojos: [\n" +
                "    { text: text1, num: 1, factor: 1.1 },\n" +
                "    { text: text2, num: 2, factor: 2.2 }\n" +
                "  ]\n" +
                "}\n";
        MyPojos mps3 = Marshallable.fromString(text);
        assertEquals(mps, mps3);

        MyPojos mps4 = Marshallable.fromFile("my-pojos.yaml");
        assertEquals(mps, mps4);

    }

    static class MyPojo extends AbstractMarshallable {
        String text;
        int num;
        double factor;

        public MyPojo(String text, int num, double factor) {
            this.text = text;
            this.num = num;
            this.factor = factor;
        }
    }

    static class MyPojos extends AbstractMarshallable {
        String name;
        List<MyPojo> myPojos = new ArrayList<>();

        public MyPojos(String name) {
            this.name = name;
        }
    }
}
