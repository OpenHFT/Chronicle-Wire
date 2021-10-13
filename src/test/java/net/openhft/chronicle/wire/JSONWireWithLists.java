package net.openhft.chronicle.wire;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * relates to https://github.com/OpenHFT/Chronicle-Wire/issues/324
 */
@Ignore
@RunWith(value = Parameterized.class)
public class JSONWireWithLists {

    private final boolean useTypes;

    @Parameterized.Parameters(name = "useTypes={0}")
    public static Collection<Object[]> wireTypes() {
        return Arrays.asList(
                new Object[]{true},
                new Object[]{false}
        );
    }

    public JSONWireWithLists(boolean useTypes) {
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

        final List<F1> drivers = Arrays.asList(new F1("Hamilton", 44), new F1("Verstappen", 33));
        jsonWire.getValueOut().object(drivers);

        final String actual = jsonWire.getValueIn().object().toString();
        Assert.assertEquals("[{surname=Hamilton, car=44}, {surname=Verstappen, car=33}]", actual);
    }
}
