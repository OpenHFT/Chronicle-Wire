package net.openhft.chronicle.wire;

public class SelfDescribingDemarshallableObject extends SelfDescribingMarshallable implements Demarshallable {

    String name = null;
    double value = Double.NaN;

    public SelfDescribingDemarshallableObject(String name, double value) {
        this.name = name;
        this.value = value;
    }

    public SelfDescribingDemarshallableObject(WireIn wire) {
        readMarshallable(wire);
    }
}
