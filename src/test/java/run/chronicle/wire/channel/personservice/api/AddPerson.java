package run.chronicle.wire.channel.personservice.api;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.converter.NanoTime;

public class AddPerson extends SelfDescribingMarshallable {

    @NanoTime
    private long time;

    private String name;

    public long time() { return time; }

    public AddPerson time( long time ) {
        this.time = time;
        return this;
    }

    public String name() { return name; }

    public AddPerson name( String name ) {
        this.name = name;
        return this;
    }

}
