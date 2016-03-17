package net.openhft.chronicle.wire;

/**
 * Created by peter on 16/03/16.
 */
public class AbstractMarshallable implements Marshallable {
    @Override
    public boolean equals(Object o) {
        return Marshallable.$equals(this, o);
    }

    @Override
    public int hashCode() {
        return Marshallable.$hashCode(this);
    }

    @Override
    public String toString() {
        return Marshallable.$toString(this);
    }
}
