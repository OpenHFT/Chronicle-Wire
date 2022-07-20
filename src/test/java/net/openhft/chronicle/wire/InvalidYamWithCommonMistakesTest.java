package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by Rob Austin
 * <p>
 * Tests that common mistakes are still parsed where we can
 */
public class InvalidYamWithCommonMistakesTest extends WireTestCommon {

    @Test
    public void testDtp() {

        DtoB expected = new DtoB("hello8");

        Marshallable actual = Marshallable.fromString("!net.openhft.chronicle.wire.InvalidYamWithCommonMistakesTest$DtoB " +
                "{\n" +
                "  y:hello8\n" +
                "}\n");
        assertEquals(expected, actual);
    }

    @Test
    public void testAssumeTheType() {

        DtoB expected = new DtoB("hello8");

        Marshallable actual = Marshallable.fromString(DtoB.class, "!InvalidYamWithCommonMistakesTest$DtoB " +
                "{\n" +
                "  y:hello8\n" +
                "}\n");
        assertEquals(expected, actual);
    }

    @Test
    public void testAssumeTheType2() {

        DtoB expected = new DtoB("hello8");

        Marshallable actual = Marshallable.fromString(DtoB.class, "!DtoB " +
                "{\n" +
                "  y:hello8\n" +
                "}\n");

        assertEquals(expected, actual);
    }

    @Test
    public void testAssumeTheTypeMissingType() {
        expectException("Cannot find a class for Xyz are you missing an alias?");
        final String cs = "!Xyz " +
                "{\n" +
                "  y: hello8\n" +
                "}\n";
        String s = Marshallable.fromString(Dto.class, cs).toString();
        assertEquals("" +
                "!net.openhft.chronicle.wire.InvalidYamWithCommonMistakesTest$Dto {\n" +
                "  y: hello8,\n" +
                "  x: !!null \"\"\n" +
                "}\n", s);
    }

    @Test
    public void testBadTypeDtp0() {

        Dto expected = new Dto("hello", new DtoB("c"));

        Dto actual = Marshallable.fromString("!net.openhft.chronicle.wire.InvalidYamWithCommonMistakesTest$Dto {\n" +
                "  x:{\n" + // strickly speaking this
                "    y: c\n" +
                "  }\n" +
                "  y: hello,\n" +
                "}");

        assertEquals(expected, actual);
    }

    @Test
    public void testBadTypeDtpBadType() {

        Dto expected = new Dto("hello", new DtoB("c"));

        Dto actual = Marshallable.fromString(Dto.class, " {\n" +
                "  x: !net.openhft.chronicle.wire.InvalidYamWithCommonMistakesTest$DtoB {\n" + //
                // strickly speaking this
                "    y: c\n" +
                "  }\n" +
                "  y: hello,\n" +
                "}");

        assertEquals(expected, actual);

    }

    @Test
    public void testAssumeTypeBasedOnWhatIsIntheYaml() {

        DtoB expected = new DtoB("hello8");

        DtoB actual = Marshallable.<DtoB>fromString("!net.openhft.chronicle.wire.InvalidYamWithCommonMistakesTest$DtoB " +
                "{\n" +
                "  y:hello8\n" +
                "}\n");

        assertEquals(expected, actual);
    }

    @Test
    public void testAssumeTypeBasedOnWhatIsIntheYaml3() {

        DtoB expected = new DtoB("hello8");

        DtoB actual = Marshallable.<DtoB>fromString(DtoB.class, "{\n" +
                "  y:hello8\n" +
                "}\n");

        assertEquals(expected, actual);
    }

    @Test
    public void testAssumeTypeBasedOnWhatIsIntheYamlWithSpace() {

        DtoB expected = new DtoB("hello8");
        Object actual = Marshallable.<DtoB>fromString(" !net.openhft.chronicle.wire.InvalidYamWithCommonMistakesTest$DtoB " +
                "{\n" +
                "  y:hello8\n" +
                "}\n");

        assertEquals(expected, actual);
    }

    @Test
    public void testAssumeTypeBasedOnWhatIsIntheYamlWithSpace2() {

        DtoB expected = new DtoB("hello8");
        Object actual = Marshallable.<DtoB>fromString(" !net.openhft.chronicle.wire.InvalidYamWithCommonMistakesTest$DtoB {\n" +
                "  y:hello8\n" +
                "}\n");

        assertEquals(expected, actual);
    }

    @Test
    public void testAssumeTypeBasedOnWhatButUseAlias() {

        ClassAliasPool.CLASS_ALIASES.addAlias(DtoB.class);
        DtoB expected = new DtoB("hello8");
        DtoB actual = Marshallable.<DtoB>fromString("!DtoB{\n" +
                "  y:hello8\n" +
                "}\n");

        assertEquals(expected, actual);
    }

    public static class Dto extends SelfDescribingMarshallable {
        String y;
        DtoB x;

        Dto(final String y, final DtoB x) {
            this.y = y;
            this.x = x;
        }

        String y() {
            return y;
        }

        DtoB x() {
            return x;
        }
    }

    public static class DtoB extends SelfDescribingMarshallable {
        String y;

        public DtoB(final String y) {
            this.y = y;
        }

        String y() {
            return y;
        }

        public DtoB y(final String y) {
            this.y = y;
            return this;
        }
    }
}
