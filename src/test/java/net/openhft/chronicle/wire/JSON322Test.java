package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * see https://github.com/OpenHFT/Chronicle-Wire/issues/322
 */
public class JSON322Test {


    public static class One extends SelfDescribingMarshallable {
        String text;

        public One(String text) {
            this.text = text;
        }
    }

    public static class Two extends SelfDescribingMarshallable {
        String text;

        public Two(String text) {
            this.text = text;
        }
    }


    public static class Four extends Two {
        String text;

        public Four(String text) {
            super(text);
            this.text = text;
        }
    }


    public static class Three extends SelfDescribingMarshallable {
        private One one;
        private Two two;

        public Three() {
        }

    }

    @Test
    public void supportNestedTypes() {

        final Three three = new Three();
        three.one = new One("hello");
        three.two = new Four("world");

        final Bytes<?> bytes = Bytes.allocateElasticOnHeap();

        JSONWire wire = new JSONWire(bytes)
                .useTypes(true);
        wire.getValueOut()
                .object(three);

        final String expected = "{\"@net.openhft.chronicle.wire.JSON322Test$Three\":{\"one\":{\"@net.openhft.chronicle.wire.JSON322Test$One\":{\"text\":\"hello\"}}, \"two\":{\"@net.openhft.chronicle.wire.JSON322Test$Four\":{\"text\":\"world\"}}  }}";
        final String actual = wire.bytes().toString();

        assertEquals(expected, actual);


        // Now try reading it back again
        final JSONWire parserWire = new JSONWire(bytes)
                .useTypes(true);

        // Potentially rename this method from outputTypes to useTypeDescription.

        //Object parsed = parserWire.getValueIn().object(Object.class);
        Object parsed = parserWire.getValueIn().object();

        assertNotNull(parsed);
        assertEquals(Three.class, parsed.getClass());

        final Three parsedThree = (Three) parsed;

        bytes.clear();
        wire.getValueOut().object(parsed);

        System.out.println("parsed = " + parsed);
        System.out.println("wire.bytes().toString() = " + wire.bytes().toString());

        System.out.println("parsedThree.one = " + parsedThree.one);
        System.out.println("parsedThree.two = " + parsedThree.two);

    }


    @Test
    public void supportTypes() {
        ClassAliasPool.CLASS_ALIASES.addAlias(Combined322.class, TypeOne322.class, TypeTwo322.class);
        Combined322 c = new Combined322();
        List<SelfDescribingMarshallable> list = c.list = new ArrayList<>();
        list.add(new TypeOne322("one"));
        list.add(new TypeTwo322(2, 22));
        c.t1 = new TypeOne322("one-one");
        c.t2 = new TypeTwo322(222, 2020);

        JSONWire wire = new JSONWire(Bytes.allocateElasticOnHeap())
                .useTypes(true);
        wire.getValueOut()
                .object(c);

        assertEquals("" +
                        "{\"@Combined322\":{" +
                        "\"t1\":{\"@TypeOne322\":{\"text\":\"one-one\"}}, " +
                        "\"t2\":{\"@TypeTwo322\":{\"id\":222,\"value\":2020}}, " +
                        "\"list\":[ {\"@TypeOne322\":{\"text\":\"one\"}}, {\"@TypeTwo322\":{\"id\":2,\"value\":22}} ]  }}",
                wire.bytes().toString());
    }

    static class Combined322 extends SelfDescribingMarshallable {
        public TypeOne322 t1;
        public TypeTwo322 t2;
        public List<SelfDescribingMarshallable> list;
    }

    static class TypeOne322 extends SelfDescribingMarshallable {
        String text;

        public TypeOne322(String one) {
            text = one;
        }
    }

    static class TypeTwo322 extends SelfDescribingMarshallable {
        int id;
        long value;

        public TypeTwo322(int id, long value) {
            this.id = id;
            this.value = value;
        }
    }
}
