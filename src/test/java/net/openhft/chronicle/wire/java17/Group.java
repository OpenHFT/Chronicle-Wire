package net.openhft.chronicle.wire.java17;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;

public class Group extends SelfDescribingMarshallable {
    private final Field field;

    public Group(Field field) {
        this.field = field;
    }
}