package net.openhft.chronicle.wire.method;

public interface Event {
    void sendingTimeNS(long sendingTimeNS);

    long sendingTimeNS();

    void transactTimeNS(long transactTimeNS);

    long transactTimeNS();
}