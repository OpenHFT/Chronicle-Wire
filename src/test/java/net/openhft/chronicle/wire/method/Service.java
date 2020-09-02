package net.openhft.chronicle.wire.method;

public enum Service  {

    S1,
    S2;

    String serviceId() {
        return toString();
    }
}
