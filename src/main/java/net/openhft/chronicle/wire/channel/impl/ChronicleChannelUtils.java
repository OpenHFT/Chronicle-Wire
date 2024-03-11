package net.openhft.chronicle.wire.channel.impl;

import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.threads.Pauser;
import net.openhft.chronicle.threads.PauserMode;
import net.openhft.chronicle.wire.channel.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * This is the ChronicleChannelUtils class.
 * The class provides utility methods related to operations and handling of the {@link ChronicleChannel}.
 * Designed as a purely static utility class, it should not be instantiated.
 */
public final class ChronicleChannelUtils {

    // Private constructor to prevent instantiation
    private ChronicleChannelUtils() {
    }

    /**
     * Creates and returns a new instance of the {@link ChronicleChannel} using the provided configurations.
     * If the initial connection receives a redirect header, the method will attempt to connect to the redirected address.
     * Once connected, the method decides on the type of channel (buffered or simple) based on the configuration.
     *
     * @param socketRegistry The socket registry to use for the new channel.
     * @param channelCfg     The configurations for the new channel.
     * @param headerOut      The header for outgoing messages.
     * @return A new instance of {@link ChronicleChannel}, either buffered or a simple connection based on configurations.
     * @throws InvalidMarshallableException if there's an error while marshalling.
     */
    public static ChronicleChannel newChannel(SocketRegistry socketRegistry,
                                              ChronicleChannelCfg<?> channelCfg,
                                              ChannelHeader headerOut,
                                              @Nullable Consumer<ChronicleChannel> closeCallback) throws InvalidMarshallableException {
        // Creation of the initial TCP connection
        TCPChronicleChannel simpleConnection = new TCPChronicleChannel(channelCfg, headerOut, socketRegistry);

        if (closeCallback != null)
            simpleConnection.closeCallback(closeCallback);

        // Retrieval of the header from the established connection
        final ChannelHeader marshallable = simpleConnection.headerIn();
        Jvm.debug().on(ChronicleChannel.class, "Client got " + marshallable);

        // Handling of a redirection scenario
        if (marshallable instanceof RedirectHeader) {
            Closeable.closeQuietly(simpleConnection);
            RedirectHeader rh = (RedirectHeader) marshallable;
            for (String location : rh.locations()) {
                try {
                    URL url = ChronicleContext.urlFor(location);
                    channelCfg.hostname(url.getHost());
                    channelCfg.port(url.getPort());
                    return newChannel(socketRegistry, channelCfg, headerOut, null);

                } catch (IORuntimeException e) {
                    Jvm.debug().on(ChronicleChannel.class, e);
                }
            }
            throw new IORuntimeException("No urls available " + rh);
        }

        // Decision on the type of Chronicle channel to return
        return channelCfg.buffered()
                ? new BufferedChronicleChannel(simpleConnection, channelCfg.pauserMode().get())
                : simpleConnection;
    }

    /**
     * Converts an event handler associated with a {@link ChronicleChannel} into a runnable task.
     * The returned task can then be executed in a separate thread. During its run, the task efficiently waits
     * for events, handling them as they occur and pausing when no events are available.
     *
     * @param chronicleChannel The channel on which events occur.
     * @param eventHandler     The event handler that processes the events.
     * @return A {@link Runnable} representation of the event handler, tailored to handle and wait for events efficiently.
     */
    @NotNull
    public static Runnable eventHandlerAsRunnable(ChronicleChannel chronicleChannel, Object eventHandler) {
        // Creation of the method reader for the provided event handler
        @SuppressWarnings("resource") final MethodReader reader = chronicleChannel.methodReader(eventHandler);

        // Determination of the closed status of the handler
        final BooleanSupplier handlerClosed;
        if (eventHandler instanceof Closeable) {
            Closeable sh = (Closeable) eventHandler;
            handlerClosed = sh::isClosed;
        } else {
            handlerClosed = () -> false;
        }

        // The main event handling logic
        return () -> {
            try {
                PauserMode pauserMode = chronicleChannel.channelCfg().pauserMode();
                if (pauserMode == null)
                    pauserMode = PauserMode.balanced;
                Pauser pauser = pauserMode.get();
                while (true) {
                    if (chronicleChannel.isClosed()) {
                        Jvm.debug().on(eventHandler.getClass(), "Reader on " + chronicleChannel + " is closed");
                        break;
                    }
                    if (handlerClosed.getAsBoolean()) {
                        Jvm.debug().on(eventHandler.getClass(), "Handler " + eventHandler + " is closed");
                        break;
                    }

                    if (reader.readOne())
                        pauser.reset();
                    else
                        pauser.pause();
                }
            } catch (Throwable t) {
                if (!chronicleChannel.isClosed() && !handlerClosed.getAsBoolean())
                    Jvm.warn().on(eventHandler.getClass(), "Error stopped reading thread", t);
            } finally {
                Closeable.closeQuietly(reader);
                Closeable.closeQuietly(eventHandler);
            }
        };
    }

}
