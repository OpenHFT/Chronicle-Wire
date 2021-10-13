package net.openhft.chronicle.wire;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

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
        final JSONWire jsonWire = new JSONWire().useTypes(useTypes);
        final F1 hamilton = new F1("Hamilton", 44);
        final Map<String, F1> expected = Collections.singletonMap("Lewis", hamilton);
        jsonWire.getValueOut().object(expected);

        Assert.assertEquals("{Lewis={surname=Hamilton, car=44}}", jsonWire.getValueIn().object().toString());
    }
}
