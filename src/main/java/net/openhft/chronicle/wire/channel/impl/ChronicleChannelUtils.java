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

import java.util.function.BooleanSupplier;

import static net.openhft.chronicle.wire.channel.ChronicleContext.*;

public final class ChronicleChannelUtils {
    private ChronicleChannelUtils() {
    }

    public static ChronicleChannel newChannel(SocketRegistry socketRegistry, ChronicleChannelCfg channelCfg, ChannelHeader headerOut) throws InvalidMarshallableException {
        TCPChronicleChannel simpleConnection = new TCPChronicleChannel(channelCfg, headerOut, socketRegistry);
        final ChannelHeader marshallable = simpleConnection.headerIn();
        Jvm.debug().on(ChronicleChannel.class, "Client got " + marshallable);
        if (marshallable instanceof RedirectHeader) {
            Closeable.closeQuietly(simpleConnection);
            RedirectHeader rh = (RedirectHeader) marshallable;

            for (String location : rh.locations()) {
                try {
                    channelCfg.clearHostnamePort();
                    urlsFor(location).forEach(url -> channelCfg.addHostnamePort(url.getHost(), url.getPort()));
                    return newChannel(socketRegistry, channelCfg, headerOut);
                } catch (IORuntimeException e) {
                    Jvm.debug().on(ChronicleChannel.class, e);
                }
            }
            throw new IORuntimeException("No urls available " + rh);
        }
        return channelCfg.buffered()
                ? new BufferedChronicleChannel(simpleConnection, channelCfg.pauserMode().get())
                : simpleConnection;
    }

    @NotNull
    public static Runnable eventHandlerAsRunnable(ChronicleChannel chronicleChannel, Object eventHandler) {
        @SuppressWarnings("resource") final MethodReader reader = chronicleChannel.methodReader(eventHandler);
        final BooleanSupplier handlerClosed;
        if (eventHandler instanceof Closeable) {
            Closeable sh = (Closeable) eventHandler;
            handlerClosed = sh::isClosed;
        } else {
            handlerClosed = () -> false;
        }

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
