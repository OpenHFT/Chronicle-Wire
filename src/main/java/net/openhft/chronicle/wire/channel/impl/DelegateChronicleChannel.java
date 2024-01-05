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

package net.openhft.chronicle.wire.channel.impl;

import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.UnrecoverableTimeoutException;
import net.openhft.chronicle.wire.WireOut;
import net.openhft.chronicle.wire.channel.*;

/**
 * This is the DelegateChronicleChannel class.
 * It acts as a delegate or a wrapper around an instance of the {@link InternalChronicleChannel},
 * effectively forwarding all method calls to the underlying channel.
 * This design allows for modification, monitoring, or extension of the behavior of the internal channel without altering its code.
 *
 * @see InternalChronicleChannel
 */
public class DelegateChronicleChannel implements InternalChronicleChannel, Closeable {

    // The internal channel instance to which method calls are delegated
    protected final InternalChronicleChannel channel;

    /**
     * Constructs a new DelegateChronicleChannel, initializing it with the provided internal channel instance.
     *
     * @param channel The underlying {@link InternalChronicleChannel} to which this delegate will forward method calls.
     */
    public DelegateChronicleChannel(InternalChronicleChannel channel) {
        this.channel = channel;
    }

    @Override
    public ChronicleChannelCfg channelCfg() {
        return channel.channelCfg();
    }

    @Override
    public ChannelHeader headerOut() {
        return channel.headerOut();
    }

    @Override
    public ChannelHeader headerIn() {
        return channel.headerIn();
    }

    @Override
    public ChannelHeader headerInToUse() {
        return channel.headerInToUse();
    }

    @Override
    public void testMessage(long now) {
        channel.testMessage(now);
    }

    @Override
    public long lastTestMessage() {
        return channel.lastTestMessage();
    }

    @Override
    public void close() {
        Closeable.closeQuietly(channel);
    }

    @Override
    public boolean isClosed() {
        return channel == null || channel.isClosed();
    }

    @Override
    public DocumentContext readingDocument() {
        return channel.readingDocument();
    }

    @Override
    public DocumentContext writingDocument(boolean metaData) throws UnrecoverableTimeoutException {
        return channel.writingDocument(metaData);
    }

    @Override
    public DocumentContext acquireWritingDocument(boolean metaData) throws UnrecoverableTimeoutException {
        return channel.acquireWritingDocument(metaData);
    }

    @Override
    public boolean supportsEventPoller() {
        return channel.supportsEventPoller();
    }

    @Override
    public EventPoller eventPoller() {
        return channel.eventPoller();
    }

    @Override
    public ChronicleChannel eventPoller(EventPoller eventPoller) {
        channel.eventPoller(eventPoller);
        return this;
    }

    @Override
    public WireOut acquireProducer() {
        return channel.acquireProducer();
    }

    @Override
    public void releaseProducer() {
        channel.releaseProducer();
    }

    @Override
    public int bufferSize() {
        return channel.bufferSize();
    }

    @Override
    public boolean recordHistory() {
        return channel.recordHistory();
    }
}
