package run.chronicle.wire.demo.mapreuse;

import java.util.ArrayList;
import java.util.List;

/**
 * This example shows how a List can be sorted and used for looking up keys
 * so that no Maps need to be created. The standard Map implementations create
 * a lot of extra objects when values are put into the Map.
 * <p>
 * The sorted List can be reused over and over again.
 */
public class SecurityLookup {

    public static void main(String[] args) {

        // These can be reused
        final Security s0 = new Security(100, 45, 2);
        final Security s1 = new Security(10, 100, 42);
        final Security s2 = new Security(20, 200, 13);

        // This can be reused
        final List<Security> securities = new ArrayList<>();

        securities.add(s0);
        securities.add(s1);
        securities.add(s2);

        // Reusable Mapper
        IntMapper<Security> mapper =
                new IntMapper<>(Security::getId);
        mapper.set(securities);

        Security security100 = mapper.get(100);

        System.out.println("security100 = " + security100);
    }

}
