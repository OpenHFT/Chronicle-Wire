package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class JSON322Test {

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
                .outputTypes(true);
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
