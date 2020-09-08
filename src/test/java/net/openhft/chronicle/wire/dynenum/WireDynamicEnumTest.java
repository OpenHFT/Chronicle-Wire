package net.openhft.chronicle.wire.dynenum;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.Mocker;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.pool.EnumCache;
import net.openhft.chronicle.wire.*;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import java.io.StringWriter;

import static net.openhft.chronicle.wire.DynamicEnum.updateEnum;
import static org.junit.Assert.*;

public class WireDynamicEnumTest {
    @Before
    public void addClassAlias() {
        ClassAliasPool.CLASS_ALIASES.addAlias(HoldsWDENum.class);
        ClassAliasPool.CLASS_ALIASES.addAlias(UnwrapsWDENum.class);
        ClassAliasPool.CLASS_ALIASES.addAlias(UsesWDENums.class);
        ClassAliasPool.CLASS_ALIASES.addAlias(WDENums.class);
    }

    @Test
    public void addedEnum() throws NoSuchFieldException {
        TextWire tw = new TextWire(Bytes.allocateElasticOnHeap());
        UsesWDENums nums = tw.methodWriter(UsesWDENums.class);
        nums.push(WDENums.ONE);

        EnumCache<WDENums> cache = EnumCache.of(WDENums.class);
        WDENums three = cache.valueOf("THREE");
        three.setField("nice", "Three");
        three.setField("value", 3);

        nums.unwraps(new UnwrapsWDENum(three));

        nums.push(three);

        nums.holds(new HoldsWDENum(WDENums.TWO, three));

        assertEquals("push: ONE\n" +
                "...\n" +
                "unwraps: {\n" +
                "  c: !WDENums {\n" +
                "    name: THREE,\n" +
                "    nice: Three,\n" +
                "    value: 3\n" +
                "  }\n" +
                "}\n" +
                "...\n" +
                "push: THREE\n" +
                "...\n" +
                "holds: {\n" +
                "  a: TWO,\n" +
                "  b: THREE\n" +
                "}\n" +
                "...\n", tw.toString());
    }

    @Test
    public void deserialize() {
        String text = "push: ONE\n" +
                "...\n" +
                "unwraps: {\n" +
                "  c: !WDENums {\n" +
                "    name: FOUR,\n" +
                "    nice: Four,\n" +
                "    value: 4\n" +
                "  }\n" +
                "}\n" +
                "...\n" +
                "push: FOUR\n" +
                "...\n" +
                "holds: {\n" +
                "  a: TWO,\n" +
                "  b: FOUR\n" +
                "}\n" +
                "...\n";
        TextWire tw = new TextWire(Bytes.from(text)).useTextDocuments();
        StringWriter sw = new StringWriter();
        MethodReader reader = tw.methodReader(Mocker.logging(UsesWDENums.class, "", sw));
        for (int i = 0; i < 4; i++)
            assertTrue(reader.readOne());
        assertFalse(reader.readOne());
        assertEquals("push[ONE]\n" +
                "unwraps[!UnwrapsWDENum {\n" +
                "  c: !WDENums {\n" +
                "    name: FOUR,\n" +
                "    nice: Four,\n" +
                "    value: 4\n" +
                "  }\n" +
                "}\n" +
                "]\n" +
                "push[FOUR]\n" +
                "holds[!HoldsWDENum {\n" +
                "  a: TWO,\n" +
                "  b: FOUR\n" +
                "}\n" +
                "]\n", sw.toString());
    }

    @Test
    public void deserialize2() {
        String text = "push: ONE\n" +
                "...\n" +
                "unwraps: {\n" +
                "  c: !WDENums {\n" +
                "    name: FIVE,\n" +
                "    nice: Five,\n" +
                "    value: 5\n" +
                "  }\n" +
                "}\n" +
                "...\n" +
                "push: FIVE\n" +
                "...\n" +
                "holds: {\n" +
                "  a: TWO,\n" +
                "  b: FIVE\n" +
                "}\n" +
                "...\n";
        TextWire tw = new TextWire(Bytes.from(text)).useTextDocuments();
        StringWriter sw = new StringWriter();
        MethodReader reader = tw.methodReader(new UsesWDENums() {
            @Override
            public void push(WDENums nums) {
                sw.append(nums.name() + " ~ " + nums.nice + " ~ " + nums.value + "\n");
            }

            @Override
            public void holds(HoldsWDENum holdsWDENum) {
                sw.append(holdsWDENum.toString());
                sw.append("# " + holdsWDENum.a.value + ", " + holdsWDENum.b.value + "\n");
            }

            @Override
            public void unwraps(UnwrapsWDENum unwrapsWDENum) {
                WDENums c = unwrapsWDENum.c;
                sw.append("Update " + c + "\n");
                updateEnum(c);
            }
        });
        for (int i = 0; i < 4; i++)
            assertTrue(reader.readOne());
        assertFalse(reader.readOne());
        assertEquals("ONE ~ One ~ 1\n" +
                "Update FIVE\n" +
                "FIVE ~ Five ~ 5\n" +
                "!HoldsWDENum {\n" +
                "  a: TWO,\n" +
                "  b: FIVE\n" +
                "}\n" +
                "# 2, 5\n", sw.toString());
    }

    enum WDENums implements WDEI, DynamicEnum {
        ONE("One", 1),
        TWO("Two", 2);

        private final String nice;
        private final int value;

        WDENums(String nice, int value) {
            this.nice = nice;
            this.value = value;
        }

        @Override
        public String nice() {
            return nice;
        }

        @Override
        public int value() {
            return value;
        }

        @Override
        public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
            DynamicEnum.super.readMarshallable(wire);
        }

        @Override
        public void writeMarshallable(@NotNull WireOut wire) {
            DynamicEnum.super.writeMarshallable(wire);
        }
    }

    interface UsesWDENums {
        void push(WDENums nums);

        void holds(HoldsWDENum holdsWDENum);

        void unwraps(UnwrapsWDENum unwrapsWDENum);
    }

    interface WDEI {
        String name();

        String nice();

        int value();
    }

    static class HoldsWDENum extends SelfDescribingMarshallable {
        WDENums a, b;

        public HoldsWDENum(WDENums a, WDENums b) {
            this.a = a;
            this.b = b;
        }
    }

    static class UnwrapsWDENum extends SelfDescribingMarshallable {
        @AsMarshallable
        WDENums c;

        public UnwrapsWDENum(WDENums c) {
            this.c = c;
        }
    }
}
