package net.openhft.chronicle.wire.channel;

import net.openhft.affinity.AffinityLock;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.core.io.ClosedIORuntimeException;
import net.openhft.chronicle.threads.NamedThreadFactory;
import net.openhft.chronicle.threads.Pauser;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.Wires;
import net.openhft.chronicle.wire.channel.impl.BufferedChronicleChannel;
import net.openhft.chronicle.wire.channel.impl.TCPChronicleChannel;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class ChronicleServiceMain extends SelfDescribingMarshallable implements Closeable {
    int port;
    Marshallable microservice;
    boolean buffered;
    transient ServerSocketChannel ssc;
    transient volatile boolean closed;
    transient Set<ChronicleChannel> channels;

    public static void main(String... args) throws IOException {
        ChronicleServiceMain main = Marshallable.fromFile(ChronicleServiceMain.class, args[0]);
        main.buffered = Jvm.getBoolean("buffered", main.buffered);
        main.run();
    }

    void run() {
        channels = Collections.newSetFromMap(new WeakHashMap<>());

        Jvm.startup().on(getClass(), "Starting " + this);
        Thread.currentThread().setName("acceptor");
        ExecutorService service = Executors.newCachedThreadPool(new NamedThreadFactory("connections"));
        try {
            ssc = ServerSocketChannel.open();
            ssc.bind(new InetSocketAddress(port));
            ChronicleChannelCfg channelCfg = new ChronicleChannelCfg().port(port);
            Function<ChannelHeader, ChannelHeader> redirectFunction = this::redirect;
            while (!isClosed()) {
                final SocketChannel sc = ssc.accept();
                sc.socket().setTcpNoDelay(true);
                final TCPChronicleChannel connection0 = new TCPChronicleChannel(SystemContext.INSTANCE, channelCfg, sc, redirectFunction);
                ChronicleChannel channel = buffered ? new BufferedChronicleChannel(connection0, Pauser.balanced(), redirectFunction) : connection0;
                channels.add(channel);
                service.submit(() -> new ConnectionHandler(channel).run());
            }
        } catch (Throwable e) {
            if (!isClosed()) Jvm.error().on(getClass(), e);
        } finally {
            close();
            Jvm.pause(50);
            // don't shut down while compiling a class
            synchronized (Wires.class) {
                // avoid shutdown while locking.
                AffinityLock.dumpLocks();
                service.shutdownNow();
            }
            try {
                service.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Jvm.warn().on(getClass(), e);
                Thread.currentThread().interrupt();
            }
        }
    }

    protected ChannelHeader redirect(ChannelHeader channelHandler) {
        if (channelHandler instanceof OkHeader)
            return new OkHeader();
        //noinspection unchecked
        return new RedirectHeader(Collections.EMPTY_LIST);
    }

    @Override
    public void close() {
        closed = true;
        Closeable.closeQuietly(ssc);
        Closeable.closeQuietly(channels);
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    class ConnectionHandler {
        final ChronicleChannel channel;

        public ConnectionHandler(ChronicleChannel channel) {
            this.channel = channel;
        }

        void run() {
            try {
                System.out.println("Server got " + channel.headerIn(ChronicleServiceMain.this::redirect));

                final Marshallable microservice = ChronicleServiceMain.this.microservice.deepCopy();
                final Field field = Jvm.getFieldOrNull(microservice.getClass(), "out");
                if (field == null)
                    throw new IllegalStateException("Microservice " + microservice + " must have a field called out");
                Object out = channel.methodWriter(field.getType());

                try (AffinityLock lock = AffinityLock.acquireLock()) {
                    field.set(microservice, out);
                    channel.eventHandlerAsRunnable(microservice).run();

                } catch (ClosedIORuntimeException e) {
                    Thread.yield();
                    if (!((Closeable) microservice).isClosed())
                        Jvm.debug().on(getClass(), "readOne threw " + e);

                } catch (Exception e) {
                    Thread.yield();
                    if (!((Closeable) microservice).isClosed() && !channel.isClosed())
                        Jvm.warn().on(getClass(), "readOne threw ", e);
                }
            } catch (Throwable t) {
                Jvm.error().on(getClass(), t);

            } finally {
                Closeable.closeQuietly(channel);
            }
        }
    }
}
