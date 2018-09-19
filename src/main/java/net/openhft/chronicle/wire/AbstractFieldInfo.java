package net.openhft.chronicle.wire;

import static net.openhft.chronicle.wire.WireType.TEXT;

/*
 * Created by peter.lawrey@chronicle.software on 24/07/2017
 */
public abstract class AbstractFieldInfo implements FieldInfo {
    protected final String name;
    protected final Class type;
    protected final BracketType bracketType;

    public AbstractFieldInfo(Class type, BracketType bracketType, String name) {
        this.type = type;
        this.bracketType = bracketType;
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Class type() {
        return type;
    }

    @Override
    public BracketType bracketType() {
        return bracketType;
    }

    @Override
    public int hashCode() {
        return HashWire.hash32(this);
    }

    @Override
    public boolean equals(Object obj) {
        return (this == obj || Wires.isEquals(this, obj));
    }

    @Override
    public String toString() {
        return TEXT.asString(this);
    }
}
