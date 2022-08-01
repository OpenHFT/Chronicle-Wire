package net.openhft.chronicle.wire.channel;

/**
 * Advanced options that may change in the future
 */
public interface InternalChronicleChannel extends ChronicleChannel {
    /**
     * @return true if eventPollers are supported by this Channel.
     */
    boolean supportsEventPoller();

    /**
     * @return the EventPoller set
     */
    EventPoller eventPoller();

    /**
     * Set an EventPoller to include in any background processing.
     *
     * @param eventPoller to use
     * @return this
     */
    ChronicleChannel eventPoller(EventPoller eventPoller);
}
