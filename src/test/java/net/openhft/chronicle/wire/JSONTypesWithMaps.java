package net.openhft.chronicle.wire;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * relates to https://github.com/OpenHFT/Chronicle-Wire/issues/324
 */
@RunWith(value = Parameterized.class)
public class JSONTypesWithMaps {

    private final boolean useTypes;

    @Parameterized.Parameters(name = "useTypes={0}")
    public static Collection<Object[]> wireTypes() {
        return Arrays.asList(
                new Object[]{true},
                new Object[]{false}
        );
    }

    public JSONTypesWithMaps(boolean useTypes) {
        this.useTypes = useTypes;
    }


    static class F1 {
        private String surname;
        private int car;

        public F1(String surname, int car) {
            this.surname = surname;
            this.car = car;
        }

        @Override
        public String toString() {
            return "{" +
                    "surname=" + surname +
                    ", car=" + car +
                    '}';
        }
    }

    @Test
    public void test() {

        final JSONWire jsonWire = new JSONWire()
                .useTypes(useTypes);

        final F1 f1 = new F1("Hamilton", 44);

        jsonWire.getValueOut()
                .object(singletonMap("Lewis", f1));

        // System.out.println(jsonWire.bytes());

        final String expected = "{Lewis=" + f1 + "}";
        final Object object = jsonWire.getValueIn().object();
        assertNotNull(object);
        assertTrue(object instanceof Map);
        final String actual = object.toString();

        Assert.assertEquals(expected, actual);
    }
}
