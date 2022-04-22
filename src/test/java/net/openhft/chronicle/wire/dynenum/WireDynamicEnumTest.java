package net.openhft.chronicle.wire.dynenum;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.Mocker;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.core.pool.EnumCache;
import net.openhft.chronicle.wire.*;
import org.junit.Before;
import org.junit.Test;

import java.io.StringWriter;

import static net.openhft.chronicle.wire.DynamicEnum.updateEnum;
import static org.junit.Assert.*;

public class WireDynamicEnumTest extends WireTestCommon {
    @Before
    public void addClassAlias() {
        ClassAliasPool.CLASS_ALIASES.addAlias(HoldsWDENum.class);
        ClassAliasPool.CLASS_ALIASES.addAlias(UnwrapsWDENum.class);
        ClassAliasPool.CLASS_ALIASES.addAlias(UnwrapsWDENum2.class);
        ClassAliasPool.CLASS_ALIASES.addAlias(UsesWDENums.class);
        ClassAliasPool.CLASS_ALIASES.addAlias(WDENums.class);
        ClassAliasPool.CLASS_ALIASES.addAlias(WDENum2.class);
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

        WDENum2 ace = new WDENum2("Ace", 101);
        nums.unwrap2(new UnwrapsWDENum2(ace));

        nums.push2(ace);

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
                "...\n" +
                "unwrap2: {\n" +
                "  d: !WDENum2 {\n" +
                "    name: ACE,\n" +
                "    nice: Ace,\n" +
                "    value: 101\n" +
                "  }\n" +
                "}\n" +
                "...\n" +
                "push2: ACE\n" +
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
                "...\n" +
                "unwrap2: {\n" +
                "  d: !WDENum2 {\n" +
                "    name: ACE,\n" +
                "    nice: Ace,\n" +
                "    value: 101\n" +
                "  }\n" +
                "}\n" +
                "...\n" +
                "push2: ACE\n" +
                "...\n";
        TextWire tw = new TextWire(Bytes.from(text)).useTextDocuments();
        StringWriter sw = new StringWriter();
        MethodReader reader = tw.methodReader(Mocker.logging(UsesWDENums.class, "", sw));
        for (int i = 0; i < 6; i++)
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
                "]\n" +
                "unwrap2[!UnwrapsWDENum2 {\n" +
                "  d: !WDENum2 {\n" +
                "    name: ACE,\n" +
                "    nice: Ace,\n" +
                "    value: 101\n" +
                "  }\n" +
                "}\n" +
                "]\n" +
                "push2[!WDENum2 {\n" +
                "  name: ACE,\n" +
                "  nice: Ace,\n" +
                "  value: 101\n" +
                "}\n" +
                "]\n", sw.toString().replace("\r", ""));
    }

    @Test
    public void deserialize2() {
        expectException("Overwriting final field name in class java.lang.Enum");

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
                "...\n" +
                "unwrap2: {\n" +
                "  d: !WDENum2 {\n" +
                "    name: KING,\n" +
                "    nice: King,\n" +
                "    value: 112\n" +
                "  }\n" +
                "}\n" +
                "...\n" +
                "push2: ONE\n" +
                "...\n" +
                "push2: TWO\n" +
                "...\n" +
                "push2: KING\n" +
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

            @Override
            public void push2(WDENum2 nums) {
                sw.append(nums.name() + " = " + nums.nice + " = " + nums.value + "\n");
            }

            @Override
            public void unwrap2(UnwrapsWDENum2 unwrapsWDENum2) {
                WDENum2 d = unwrapsWDENum2.d;
                sw.append("Update " + d + "\n");
                updateEnum(d);
            }
        });
        for (int i = 0; i < 8; i++)
            assertTrue(reader.readOne());
        assertFalse(reader.readOne());
        assertEquals("ONE ~ One ~ 1\n" +
                "Update FOUR\n" +
                "FOUR ~ Four ~ 4\n" +
                "!HoldsWDENum {\n" +
                "  a: TWO,\n" +
                "  b: FOUR\n" +
                "}\n" +
                "# 2, 4\n" +
                "Update !WDENum2 {\n" +
                "  name: KING,\n" +
                "  nice: King,\n" +
                "  value: 112\n" +
                "}\n" +
                "\n" +
                "ONE = One = 1\n" +
                "TWO = Two = 2\n" +
                "KING = King = 112\n", sw.toString());
    }

    enum WDENums implements WDEI, DynamicEnum {
        ONE("One", 1),
        TWO("Two", 2);

        private String nice;
        private int value;

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

    }

    interface UsesWDENums {
        void push(WDENums nums);

        void push2(WDENum2 nums);

        void holds(HoldsWDENum holdsWDENum);

        void unwraps(UnwrapsWDENum unwrapsWDENum);

        void unwrap2(UnwrapsWDENum2 unwrapsWDENum2);
    }

    interface WDEI {
        String name();

        String nice();

        int value();
    }

    static class WDENum2 extends SelfDescribingMarshallable implements WDEI, DynamicEnum {
        static final WDENum2 ONE = new WDENum2("One", 1);
        static final WDENum2 TWO = new WDENum2("Two", 2);

        private String name;
        private String nice;
        private int value;

        WDENum2(String nice, int value) {
            this.name = nice.toUpperCase();
            this.nice = nice;
            this.value = value;
        }

        @Override
        public String name() {
            return name;
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
        public int ordinal() {
            return -1;
        }
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

    static class UnwrapsWDENum2 extends SelfDescribingMarshallable {
        @AsMarshallable
        WDENum2 d;

        public UnwrapsWDENum2(WDENum2 d) {
            this.d = d;
        }
    }
}
