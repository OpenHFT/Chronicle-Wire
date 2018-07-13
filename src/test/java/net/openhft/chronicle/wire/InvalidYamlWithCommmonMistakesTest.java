package net.openhft.chronicle.wire;

import org.junit.Test;

/**
 * Created by Rob Austin
 *
 * Tests that common mistakes are still parsed where we can
 */
public class InvalidYamlWithCommmonMistakesTest {

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

        Marshallable.fromString("!software.chronicle.services.config.BadYamlParseTest$Dto " +
                "{\n" +
                "  x:hello\n" +
                "  y:hello8\n" +
                "}\n");
    }

    @Test
    public void testBadTypeDtp0() {

        Marshallable.fromString("!software.chronicle.services.config.BadYamlParseTest$Dto {\n" +
                "  x:{\n" + // strickly speaking this
                "    y: c\n" +
                "  }\n" +
                "  y: hello,\n" +
                "}");

    }                                                                                                              

}
