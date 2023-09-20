package net.openhft.chronicle.wire.channel;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;

/**
 * Represents a configuration object for specifying the hostname and port of a target destination.
 *
 * <p>The `HostPortCfg` class is designed to encapsulate the details of a network host's name and its associated port.
 * By default, if the hostname is not provided, it defaults to "localhost".
 *
 * @since 2023-09-15
 */
public class HostPortCfg extends SelfDescribingMarshallable {

    // The network host's name. It defaults to "localhost" if not specified.
    private String hostname;

    // The port associated with the hostname.
    private int port;

    /**
     * Constructs a new configuration with the specified hostname and port.
     *
     * @param hostname The network host's name. If null, defaults to "localhost".
     * @param port The port associated with the hostname.
     */
    public HostPortCfg(String hostname, int port) {
        this.hostname = hostname == null ? "localhost" : hostname;
        this.port = port;
    }

    /**
     * Retrieves the configured hostname.
     *
     * @return The network host's name.
     */
    public String hostname() {
        return hostname;
    }

    /**
     * Retrieves the configured port.
     *
     * @return The port associated with the hostname.
     */
    public int port() {
        return port;
    }
}
