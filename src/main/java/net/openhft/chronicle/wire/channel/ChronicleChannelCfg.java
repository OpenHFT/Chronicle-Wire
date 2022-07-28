package net.openhft.chronicle.wire.channel;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.threads.PauserMode;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.channel.impl.internal.Handler;

public class ChronicleChannelCfg<C extends ChronicleChannelCfg<C>> extends SelfDescribingMarshallable {
    static {
        Handler.init();
    }

    private boolean initiator;
    private boolean buffered;
    private PauserMode pauser = PauserMode.yielding;
    private String hostname;
    private int port;

    private double connectionTimeoutSecs = 1.0;

    public ChronicleChannelCfg initiator(boolean initiator) {
        this.initiator = initiator;
        return this;
    }

    public boolean initiator() {
        return initiator;
    }

    public String hostname() {
        return hostname;
    }

    public C hostname(String hostname) {
        this.hostname = hostname;
        return (C) this;
    }

    public int port() {
        return port;
    }

    public C port(int port) {
        this.port = port;
        return (C) this;
    }

    public boolean buffered() {
        return buffered;
    }

    public C buffered(boolean buffered) {
        this.buffered = buffered;
        return (C) this;
    }

    public PauserMode pauserMode() {
        return pauser;
    }

    public C pauserMode(PauserMode pauser) {
        this.pauser = pauser;
        return (C) this;
    }

    public double connectionTimeoutSecs() {
        if (connectionTimeoutSecs <= 0 || Jvm.isDebug())
            return 60;
        return connectionTimeoutSecs;
    }

    public C connectionTimeoutSecs(double connectionTimeoutSecs) {
        this.connectionTimeoutSecs = connectionTimeoutSecs;
        return (C) this;
    }
}
