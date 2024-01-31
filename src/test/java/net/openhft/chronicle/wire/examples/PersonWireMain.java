package net.openhft.chronicle.wire.examples;

import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.converter.ShortText;

import static net.openhft.chronicle.core.time.SystemTimeProvider.CLOCK;

public class PersonWireMain {
    public static void main(String[] args) {
        // use the short name of the class without needing the package
        ClassAliasPool.CLASS_ALIASES.addAlias(Person.class);

        Person p1 = new Person()
                .name("George Ball")
                .timestampNS(CLOCK.currentTimeNanos())
                .userName(ShortText.INSTANCE.parse("georgeb"));
        System.out.println("p1: " + p1);

        Wire yWire = Wire.newYamlWireOnHeap();
        p1.writeMarshallable(yWire);
        System.out.println(yWire);

        Person p2 = new Person();
        p2.readMarshallable(yWire);
        System.out.println("p2: " + p2);

        yWire.clear();

        final PersonOps personOps = yWire.methodWriter(PersonOps.class);
        personOps.addPerson(
                Marshallable.fromString(Person.class, "" +
                        "name: Alice Smith\n" +
                        "timestampNS: 2022-11-11T10:11:54.7071341\n" +
                        "userName: alices\n"));

        personOps.addPerson(p1);

        personOps.addPerson(new Person()
                .name("Bob Singh")
                .timestampNS(CLOCK.currentTimeNanos())
                .userName(ShortText.INSTANCE.parse("bobs")));

        System.out.println(yWire);

        MethodReader reader = yWire.methodReader(
                (PersonOps) p -> System.out.println("added " + p));
        for (int i = 0; i < 3; i++)
            reader.readOne();
    }
}
