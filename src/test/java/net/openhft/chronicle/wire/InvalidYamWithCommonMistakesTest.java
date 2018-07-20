package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Rob Austin
 *
 * Tests that common mistakes are still parsed where we can
 */
public class InvalidYamWithCommonMistakesTest {

    public static class Dto extends AbstractMarshallable {
        String y;
        DtoB x;

        String y() {
            return y;
        }

        DtoB x() {
            return x;
        }

        Dto(final String y, final DtoB x) {
            this.y = y;
            this.x = x;
        }
    }

    public static class DtoB extends AbstractMarshallable {
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

    @Test
    public void testDtp() {

        DtoB expected = new DtoB("hello8");

        Marshallable actual = Marshallable.fromString("!net.openhft.chronicle.wire.InvalidYamWithCommonMistakesTest$DtoB " +
                "{\n" +
                "  y:hello8\n" +
                "}\n");
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testAssumeTheType() {

        DtoB expected = new DtoB("hello8");

        Marshallable actual = Marshallable.fromString(DtoB.class, "!InvalidYamWithCommonMistakesTest$DtoB " +
                "{\n" +
                "  y:hello8\n" +
                "}\n");
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testAssumeTheType2() {

        DtoB expected = new DtoB("hello8");

        Marshallable actual = Marshallable.fromString(DtoB.class, "!DtoB " +
                "{\n" +
                "  y:hello8\n" +
                "}\n");

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testAssumeTheTypeMissingType() {

        Marshallable.fromString(Dto.class, "Dto " +
                "{\n" +
                "  y:hello8\n" +
                "}\n");

    }


    @Test
    public void testBadTypeDtp0() {

        Marshallable.fromString("!net.openhft.chronicle.wire.InvalidYamlWithCommmonMistakesTest$Dto {\n" +
                "  x:{\n" + // strickly speaking this
                "    y: c\n" +
                "  }\n" +
                "  y: hello,\n" +
                "}");

    }

    @Test
    public void testAssumeTypeBasedOnWhatIsIntheYaml() {

        DtoB expected = new DtoB("hello8");

        DtoB actual = Marshallable.<DtoB>fromString("!net.openhft.chronicle.wire.InvalidYamWithCommonMistakesTest$DtoB " +
                "{\n" +
                "  y:hello8\n" +
                "}\n");

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testAssumeTypeBasedOnWhatIsIntheYaml3() {

        DtoB expected = new DtoB("hello8");

        DtoB actual = Marshallable.<DtoB>fromString(DtoB.class, "{\n" +
                "  y:hello8\n" +
                "}\n");

        Assert.assertEquals(expected, actual);
    }


    @Test
    public void testAssumeTypeBasedOnWhatIsIntheYamlWithSpace() {

        DtoB expected = new DtoB("hello8");
        Object actual = Marshallable.<DtoB>fromString(" !net.openhft.chronicle.wire.InvalidYamWithCommonMistakesTest$DtoB " +
                "{\n" +
                "  y:hello8\n" +
                "}\n");

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testAssumeTypeBasedOnWhatIsIntheYamlWithSpace2() {

        DtoB expected = new DtoB("hello8");
        Object actual = Marshallable.<DtoB>fromString(" !net.openhft.chronicle.wire.InvalidYamWithCommonMistakesTest$DtoB {\n" +
                "  y:hello8\n" +
                "}\n");

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testAssumeTypeBasedOnWhatButUseAlias() {

        ClassAliasPool.CLASS_ALIASES.addAlias(DtoB.class);
        DtoB expected = new DtoB("hello8");
        DtoB actual = Marshallable.<DtoB>fromString("!DtoB{\n" +
                "  y:hello8\n" +
                "}\n");

        Assert.assertEquals(expected, actual);
    }

}
