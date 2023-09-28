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

import net.openhft.affinity.AffinityThreadFactory;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.threads.NamedThreadFactory;
import net.openhft.chronicle.threads.Pauser;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.UnrecoverableTimeoutException;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireOut;
import net.openhft.chronicle.wire.channel.EventPoller;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static net.openhft.chronicle.core.io.Closeable.closeQuietly;
import static net.openhft.chronicle.wire.channel.impl.TCPChronicleChannel.validateHeader;

public class BufferedChronicleChannel extends DelegateChronicleChannel {
    private static final boolean ALLOW_AFFINITY = Jvm.getBoolean("useAffinity", true);
    private final Pauser pauser;
    private final WireExchanger exchanger = new WireExchanger();
    private final ExecutorService bgWriter;
    private volatile EventPoller eventPoller;

    public BufferedChronicleChannel(TCPChronicleChannel channel, Pauser pauser) {
        super(channel);
        this.pauser = pauser;

        String desc = channel.connectionCfg().initiator() ? "init" : "accp";
        final String writer = desc + "-writer";
        final ThreadFactory factory = ALLOW_AFFINITY && pauser.isBusy()
                ? new AffinityThreadFactory(writer, true)
                : new NamedThreadFactory(writer, true);
        bgWriter = Executors.newSingleThreadExecutor(factory);
        bgWriter.submit(this::bgWrite);
    }

    @Override
    public EventPoller eventPoller() {
        return eventPoller;
    }

    @Override
    public BufferedChronicleChannel eventPoller(EventPoller eventPoller) {
        if (isClosed())
            throw new ClosedIllegalStateException(this.getClass().getName() + " closed for " + Thread.currentThread().getName());
        this.eventPoller = eventPoller;
        return this;
    }

    private void bgWrite() {
        try {
            final TCPChronicleChannel channel = (TCPChronicleChannel) this.channel;
            while (!isClosing()) {
                channel.checkConnected();
                final Wire wire = exchanger.acquireConsumer();
                if (wire.bytes().isEmpty()) {
                    final EventPoller eventPoller = this.eventPoller();
                    final boolean idle = eventPoller == null || !eventPoller.onPoll(this);
                    exchanger.releaseConsumer();
                    if (idle)
                        pauser.pause();
                    continue;
                }
                assert validateHeader(wire.bytes().peekVolatileInt());
                // System.out.println("Writing - " + Wires.fromSizePrefixedBlobs(wire));
                pauser.reset();
                channel.flushOut(wire);
                exchanger.releaseConsumer();
            }
        } catch (Throwable t) {
            // don't rely on the closer calling close() in the right order
            Thread.yield();
            if (!isClosing() && !channel.isClosing())
                Jvm.warn().on(getClass(), "bgWriter died", t);
        } finally {
            bgWriter.shutdown();
            closeQuietly(eventPoller());
        }
    }

    @Override
    public DocumentContext writingDocument(boolean metaData) throws UnrecoverableTimeoutException {
        return exchanger.writingDocument(metaData);
    }

    @Override
    public DocumentContext acquireWritingDocument(boolean metaData) throws UnrecoverableTimeoutException {
        return exchanger.acquireWritingDocument(metaData);
    }

    @Override
    public WireOut acquireProducer() {
        return exchanger.acquireProducer();
    }

    @Override
    public void releaseProducer() {
        exchanger.releaseProducer();
    }

    @Override
    public void close() {
        super.close();
        closeQuietly(eventPoller, exchanger);
    }
}
