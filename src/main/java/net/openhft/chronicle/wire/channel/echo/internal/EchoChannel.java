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

package net.openhft.chronicle.wire.channel.echo.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.SimpleCloseable;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.UnrecoverableTimeoutException;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;
import net.openhft.chronicle.wire.channel.ChannelHeader;
import net.openhft.chronicle.wire.channel.ChronicleChannel;
import net.openhft.chronicle.wire.channel.ChronicleChannelCfg;
import net.openhft.chronicle.wire.channel.OkHeader;
import net.openhft.chronicle.wire.converter.NanoTime;

/**
 * Represents an echo channel that extends the capabilities of a {@link SimpleCloseable} and
 * implements the {@link ChronicleChannel} interface. The primary responsibility of this class is
 * to manage the reading and writing of documents to a {@link Wire} while keeping track of test messages
 * and headers.
 */
public class EchoChannel extends SimpleCloseable implements ChronicleChannel {

    // A default OK header that signifies a successful operation or message
    private static final OkHeader OK = new OkHeader();

    // The wire used for reading and writing documents
    private final Wire wire = WireType.BINARY_LIGHT.apply(Bytes.allocateElasticOnHeap());

    // Configuration associated with the channel
    private final ChronicleChannelCfg channelCfg;

    // Keeps track of the last test message's timestamp
    private long lastTestMessage;

    /**
     * Constructs an EchoChannel with the provided channel configuration.
     *
     * @param channelCfg The configuration for this channel.
     */
    public EchoChannel(ChronicleChannelCfg channelCfg) {
        this.channelCfg = channelCfg;
    }

    @Override
    public ChronicleChannelCfg channelCfg() {
        return channelCfg;
    }

    @Override
    public ChannelHeader headerOut() {
        return OK;
    }

    @Override
    public ChannelHeader headerIn() {
        return OK;
    }

    @Override
    public void testMessage(long now) {
        this.lastTestMessage = now;
        try (DocumentContext dc = writingDocument(true)) {
            dc.wire().write("testMessage").writeLong(NanoTime.INSTANCE, now);
        }
    }

    @Override
    public long lastTestMessage() {
        return lastTestMessage;
    }

    @Override
    public DocumentContext readingDocument() {
        return wire.readingDocument();
    }

    @Override
    public DocumentContext writingDocument(boolean metaData) throws UnrecoverableTimeoutException {
        if (wire.isEmpty())
            wire.reset();
        return wire.writingDocument(metaData);
    }

    @Override
    public DocumentContext acquireWritingDocument(boolean metaData) throws UnrecoverableTimeoutException {
        if (wire.isEmpty())
            wire.reset();
        return wire.acquireWritingDocument(metaData);
    }
}
