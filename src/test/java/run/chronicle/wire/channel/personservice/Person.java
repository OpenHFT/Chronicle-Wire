package run.chronicle.wire.channel.personservice;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.converter.NanoTime;

public class Person extends SelfDescribingMarshallable {
    @NanoTime
    private long timestampNS;
    private String name;

    public long timestampNS() {
        return this.timestampNS;
    }

    public Person timestampNS( long ts ) {
        this.timestampNS = ts;
        return this;
    }

    public String name() {
        return this.name;
    }

    public Person name( String name ) {
        this.name = name;
        return this;
    }

    @Override
    public String toString() {
        return "Person " + "{" +
            " timestampNS: " + timestampNS +
            ", name: '" + name + "'" +
            " }";
    }
}
