/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire.channel;

import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.core.io.ClosedIORuntimeException;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.util.StringUtils;
import net.openhft.chronicle.wire.*;
import net.openhft.chronicle.wire.channel.impl.ChronicleChannelUtils;
import net.openhft.chronicle.wire.channel.impl.SocketRegistry;
import net.openhft.chronicle.wire.converter.NanoTime;

import java.util.function.Consumer;

/**
 * The ChronicleChannel interface encapsulates a communication channel that can process various data types.
 * It extends the Closeable, MarshallableOut, and MarshallableIn interfaces, thereby supporting a wide range of I/O operations.
 */
@Deprecated(/* to be moved in x.27 */)
public interface ChronicleChannel extends Closeable, MarshallableOut, MarshallableIn {

    /**
     * Creates a new instance of a ChronicleChannel.
     *
     * @param socketRegistry the SocketRegistry for managing the socket
     * @param channelCfg     the ChronicleChannelCfg providing the configuration for the channel
     * @param headerOut      the ChannelHeader for outgoing messages
     * @return a new ChronicleChannel instance
     * @throws InvalidMarshallableException if there's an error marshalling the objects for communication
     */
    static ChronicleChannel newChannel(SocketRegistry socketRegistry, ChronicleChannelCfg<?> channelCfg, ChannelHeader headerOut) throws InvalidMarshallableException {
        return ChronicleChannelUtils.newChannel(socketRegistry, channelCfg, headerOut, null);
    }

    static ChronicleChannel newChannel(SocketRegistry socketRegistry, ChronicleChannelCfg<?> channelCfg, ChannelHeader headerOut,  Consumer<ChronicleChannel> closeCallback) throws InvalidMarshallableException {
        return ChronicleChannelUtils.newChannel(socketRegistry, channelCfg, headerOut, closeCallback);
    }

    /**
     * Retrieves the configuration of the channel.
     *
     * @return the ChronicleChannelCfg instance representing the channel configuration
     */
    ChronicleChannelCfg<?> channelCfg();

    /**
     * Retrieves the header for outgoing messages.
     *
     * @return the ChannelHeader instance representing the header for outgoing messages
     */
    ChannelHeader headerOut();

    /**
     * Retrieves the header for incoming messages.
     *
     * @return the ChannelHeader instance representing the header for incoming messages
     */
    ChannelHeader headerIn();

    /**
     * Reads a single event of the expected type from the channel.
     *
     * @param eventType    a StringBuilder object to append the event type
     * @param expectedType the Class of the expected event type
     * @return an instance of the expected type representing the read data
     * @throws ClosedIORuntimeException     if this ChronicleChannel has been closed
     * @throws InvalidMarshallableException if the Marshallable object fails to be read
     */
    default <T> T readOne(StringBuilder eventType, Class<T> expectedType) throws ClosedIORuntimeException, InvalidMarshallableException {
        while (!isClosed()) {
            try (DocumentContext dc = readingDocument()) {
                if (dc.isPresent()) {
                    ValueIn in = dc.wire().read(eventType);
                    if (StringUtils.isEqual(eventType, MethodReader.HISTORY) ||
                            StringUtils.isEqual(eventType, "" + MethodReader.MESSAGE_HISTORY_METHOD_ID)) {
                        in.object(MessageHistory.get(), VanillaMessageHistory.class);
                        in = dc.wire().read(eventType);
                    }
                    return in.object(expectedType);
                }
            }
        }
        throw new ClosedIORuntimeException("Closed");
    }

    /**
     * Creates a Runnable that reads all events from the channel and delegates them to the provided event handler.
     *
     * @param eventHandler an object that handles the processed events
     * @return a Runnable instance that can be submitted to a Thread or ExecutorService
     */
    default Runnable eventHandlerAsRunnable(Object eventHandler) {
        return ChronicleChannelUtils.eventHandlerAsRunnable(this, eventHandler);
    }

    /**
     * Sends a test message using a monotonically increasing timestamp, enabling the caller to wait for a response via lastTestMessage().
     *
     * @param now a monotonically increasing timestamp
     */
    void testMessage(@NanoTime long now);

    /**
     * Retrieves the highest timestamp received from the test messages.
     *
     * @return the highest timestamp received as a long
     */
    long lastTestMessage();
}
