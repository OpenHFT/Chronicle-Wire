package net.openhft.chronicle.wire;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static java.util.Collections.singletonMap;

/**
 * relates to https://github.com/OpenHFT/Chronicle-Wire/issues/324
 */
@Ignore
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
    }

    @Test
    public void test() {

        final JSONWire jsonWire = new JSONWire()
                .useTypes(useTypes);

        jsonWire.getValueOut()
                .object(singletonMap("Lewis", new F1("Hamilton", 44)));

        final String actual = jsonWire.getValueIn().object().toString();
        Assert.assertEquals("{Lewis={surname=Hamilton, car=44}}", actual);
    }
}
