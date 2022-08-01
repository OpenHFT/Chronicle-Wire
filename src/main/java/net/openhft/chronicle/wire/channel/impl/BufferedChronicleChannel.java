package net.openhft.chronicle.wire.channel.impl;

import net.openhft.affinity.AffinityThreadFactory;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.threads.NamedThreadFactory;
import net.openhft.chronicle.threads.Pauser;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.UnrecoverableTimeoutException;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.channel.ChannelHeader;
import net.openhft.chronicle.wire.channel.EventPoller;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;

import static net.openhft.chronicle.wire.channel.impl.TCPChronicleChannel.validateHeader;

public class BufferedChronicleChannel extends DelegateChronicleChannel {
    private final Pauser pauser;
    private final WireExchanger exchanger = new WireExchanger();
    private final ExecutorService bgWriter;
    private final int lingerNs;
    private final Function<ChannelHeader, ChannelHeader> redirectFunction;
    private volatile EventPoller eventPoller;

    public BufferedChronicleChannel(TCPChronicleChannel channel, Pauser pauser, Function<ChannelHeader, ChannelHeader> redirectFunction) {
        this(channel, pauser, redirectFunction, 8);
    }

    public BufferedChronicleChannel(TCPChronicleChannel channel, Pauser pauser, Function<ChannelHeader, ChannelHeader> redirectFunction, int lingerUs) {
        super(channel);
        this.pauser = pauser;
        this.redirectFunction = redirectFunction;

        lingerNs = lingerUs * 1000;
        String desc = channel.connectionCfg().initiator() ? "init" : "accp";
        final String writer = desc + "~writer";
        final ThreadFactory factory = pauser.isBusy()
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
        this.eventPoller = eventPoller;
        return this;
    }

    private void bgWrite() {
        try {
            final TCPChronicleChannel channel = (TCPChronicleChannel) this.channel;
            while (!isClosing()) {
                long start = System.nanoTime();
                channel.checkConnected(redirectFunction);
                final Wire wire = exchanger.acquireConsumer();
                if (wire.bytes().isEmpty()) {
                    final EventPoller eventPoller = this.eventPoller();
                    if (eventPoller == null || !eventPoller.onPoll(this)) {
                        pauser.pause();
                    }
                    exchanger.releaseConsumer();
                    continue;
                }
                assert validateHeader(wire.bytes().peekVolatileInt());
                // System.out.println("Writing - " + Wires.fromSizePrefixedBlobs(wire));
                pauser.reset();
//                long size = wire.bytes().readRemaining();
                channel.flushOut(wire);
                exchanger.releaseConsumer();
                while (System.nanoTime() < start + lingerNs) {
                    pauser.pause();
                }
            }
        } catch (Throwable t) {
            if (!isClosing())
                Jvm.warn().on(getClass(), "bgWriter died", t);
        } finally {
            bgWriter.shutdown();
            Closeable.closeQuietly(eventPoller());
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
    public void close() {
        super.close();
        exchanger.close();
    }
}
