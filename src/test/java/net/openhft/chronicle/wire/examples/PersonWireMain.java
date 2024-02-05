package net.openhft.chronicle.wire.examples;

import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.converter.ShortText;

import static net.openhft.chronicle.core.time.SystemTimeProvider.CLOCK;

public class PersonWireMain {
    public static void main(String[] args) {
        // Add class alias for "Person" so that the short name can be used without package information
        ClassAliasPool.CLASS_ALIASES.addAlias(Person.class);

        // Initialize a new person instance with some values
        Person p1 = new Person()
                .name("George Ball")
                .timestampNS(CLOCK.currentTimeNanos())
                .userName(ShortText.INSTANCE.parse("georgeb"));
        System.out.println("p1: " + p1);

        // Create a new YAML wire on heap for serialization
        Wire yWire = Wire.newYamlWireOnHeap();

        // Serialize the person instance to the wire
        p1.writeMarshallable(yWire);
        System.out.println(yWire);

        // Deserialize a new person instance from the wire
        Person p2 = new Person();
        p2.readMarshallable(yWire);
        System.out.println("p2: " + p2);

        // Clear the wire for reuse
        yWire.clear();

        // Create a method writer for the PersonOps interface
        final PersonOps personOps = yWire.methodWriter(PersonOps.class);

        // Add a new person using the method writer and a string representation
        personOps.addPerson(
                Marshallable.fromString(Person.class, "" +
                        "name: Alice Smith\n" +
                        "timestampNS: 2022-11-11T10:11:54.7071341\n" +
                        "userName: alices\n"));

        // Add the first person using the method writer
        personOps.addPerson(p1);

        // Add another new person with some values using the method writer
        personOps.addPerson(new Person()
                .name("Bob Singh")
                .timestampNS(CLOCK.currentTimeNanos())
                .userName(ShortText.INSTANCE.parse("bobs")));

        // Print the wire content after adding all persons
        System.out.println(yWire);

        // Create a method reader to consume messages from the wire and print added persons
        MethodReader reader = yWire.methodReader(
                (PersonOps) p -> System.out.println("added " + p));

        // Read and process all messages from the wire
        for (int i = 0; i < 3; i++)
            reader.readOne();
    }
}
