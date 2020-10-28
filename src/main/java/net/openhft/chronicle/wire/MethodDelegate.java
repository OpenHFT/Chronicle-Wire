package net.openhft.chronicle.wire;

public interface MethodDelegate<OUT> {
    void delegate(OUT delegate);
}
