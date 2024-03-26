package net.openhft.chronicle.wire.channel;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;

public class HostPortCfg extends SelfDescribingMarshallable {
    private String hostname;
    private int port;

    public HostPortCfg(String hostname, int port) {
        this.hostname = hostname == null ? "localhost" : hostname;
        this.port = port;
    }

    public String hostname() {
        return hostname;
    }

    public int port() {
        return port;
    }
}
