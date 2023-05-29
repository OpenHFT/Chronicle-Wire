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

import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.core.io.ClosedIORuntimeException;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.MarshallableIn;
import net.openhft.chronicle.wire.MarshallableOut;
import net.openhft.chronicle.wire.channel.impl.ChronicleChannelUtils;
import net.openhft.chronicle.wire.channel.impl.SocketRegistry;
import net.openhft.chronicle.wire.converter.NanoTime;

/**
 * ChronicleChannel provides an interface for a communication channel that can handle various types of data.
 * It extends Closeable, MarshallableOut, and MarshallableIn, which allows it to be used for a wide range of I/O operations.
 */
public interface ChronicleChannel extends Closeable, MarshallableOut, MarshallableIn {

    /**
     * Returns a new instance of a ChronicleChannel.
     *
     * @param socketRegistry the socket registry to use
     * @param channelCfg     the configuration for the channel
     * @param headerOut      the header for outgoing messages
     * @throws InvalidMarshallableException if the Marshallable object is invalid
     */
    static ChronicleChannel newChannel(SocketRegistry socketRegistry, ChronicleChannelCfg channelCfg, ChannelHeader headerOut) throws InvalidMarshallableException {
        return ChronicleChannelUtils.newChannel(socketRegistry, channelCfg, headerOut);
    }

    /**
     * Returns the configuration of the channel.
     *
     * @return the channel configuration
     */
    ChronicleChannelCfg channelCfg();

    /**
     * Returns the header for outgoing messages.
     *
     * @return the header for outgoing messages
     */
    ChannelHeader headerOut();

    /**
     * Returns the header for incoming messages.
     *
     * @return the header for incoming messages
     */
    ChannelHeader headerIn();

    /**
     * Reads a single event of the expected type.
     *
     * @param eventType    of the event read
     * @param expectedType the class of the expected event type
     * @return any data transfer object
     * @throws ClosedIORuntimeException     if this ChronicleChannel is closed
     * @throws InvalidMarshallableException if the Marshallable object is invalid
     */
    default <T> T readOne(StringBuilder eventType, Class<T> expectedType) throws ClosedIORuntimeException, InvalidMarshallableException {
        while (!isClosed()) {
            try (DocumentContext dc = readingDocument()) {
                if (dc.isPresent()) {
                    return dc.wire().read(eventType).object(expectedType);
                }
            }
        }
        throw new ClosedIORuntimeException("Closed");
    }

    /**
     * Returns a Runnable that reads all events and calls the corresponding method on the event handler.
     *
     * @param eventHandler to handle events
     * @return a Runnable that can be passed to a Thread or ExecutorService
     */
    default Runnable eventHandlerAsRunnable(Object eventHandler) {
        return ChronicleChannelUtils.eventHandlerAsRunnable(this, eventHandler);
    }

    /**
     * Send a test message so the caller can wait for the response via lastTestMessage()
     *
     * @param now a monotonically increasing timestamp
     */
    void testMessage(@NanoTime long now);

    /**
     * @return the highest timestamp received
     */
    long lastTestMessage();
}
