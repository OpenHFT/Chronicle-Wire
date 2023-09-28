package net.openhft.chronicle.wire.examples;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.converter.Base85;
import net.openhft.chronicle.wire.converter.NanoTime;

public class Person extends SelfDescribingMarshallable {
    private String name;
    @NanoTime
    private long timestampNS;
    @Base85
    private long userName;

    public String name() {
        return name;
    }

    public Person name(String name) {
        this.name = name;
        return this;
    }

    public long timestampNS() {
        return timestampNS;
    }

    public Person timestampNS(long timestampNS) {
        this.timestampNS = timestampNS;
        return this;
    }

    public long userName() {
        return userName;
    }

    public Person userName(long userName) {
        this.userName = userName;
        return this;
    }
}
