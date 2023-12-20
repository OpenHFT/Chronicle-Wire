/*
 * Copyright 2016-2020 chronicle.software
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
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.bytes.MethodReaderInterceptorReturns;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.util.Mocker;
import net.openhft.chronicle.wire.utils.MethodReaderStatus;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.lang.ThreadLocal.withInitial;
import static net.openhft.chronicle.core.io.Closeable.closeQuietly;

/**
 * Base class for generated method readers.
 */
public abstract class AbstractGeneratedMethodReader implements MethodReader {
    private static final Consumer<MessageHistory> NO_OP_MH_CONSUMER = Mocker.ignored(Consumer.class);
    public final static ThreadLocal<String> SERVICE_NAME = new ThreadLocal<>();
    private final static ConcurrentHashMap<String, MessageHistoryThreadLocal> TEMP_MESSAGE_HISTORY_BY_SERVICE_NAME = new ConcurrentHashMap<>();
    protected final WireParselet debugLoggingParselet;
    private final MarshallableIn in;
    private final MessageHistoryThreadLocal tempMessageHistory;
    protected MessageHistory messageHistory;
    protected boolean dataEventProcessed;
    private boolean closeIn = false;
    private boolean closed;
    private Consumer<MessageHistory> historyConsumer = NO_OP_MH_CONSUMER;

    private Predicate predicate;
    private boolean scanning;

    protected AbstractGeneratedMethodReader(MarshallableIn in,
                                            WireParselet debugLoggingParselet) {
        this.in = in;
        this.debugLoggingParselet = debugLoggingParselet;

        // gets the name of the service so we can offer history message caching

        // the services name is set by the chronicle services framework
        String serviceName = SERVICE_NAME.get();
        if (serviceName == null)
            serviceName = "";

        // this was handled to support when multiple services are using the same thread.
        this.tempMessageHistory = TEMP_MESSAGE_HISTORY_BY_SERVICE_NAME.computeIfAbsent(serviceName, x -> new MessageHistoryThreadLocal());
    }

    public AbstractGeneratedMethodReader predicate(Predicate predicate) {
        this.predicate = predicate;
        return this;
    }

    /**
     * Helper method used by implementations to get a Method
     */
    protected static Method lookupMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        try {
            final Method method = clazz.getMethod(name, parameterTypes);

            Jvm.setAccessible(method);

            return method;
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * @param historyConsumer sets a history consumer, which will be called the next message if for a different queue
     *                        and the history message is not written to the output queue.
     *                        This allows LAST_WRITTEN to still work when there is no output for a give message
     */
    public void historyConsumer(Consumer<MessageHistory> historyConsumer) {
        this.historyConsumer = historyConsumer;
    }

    /**
     * Reads call name and arguments from the wire and performs invocation on a target object instance.
     * Implementation of this method is generated in runtime, see {@link GenerateMethodReader}.
     *
     * @param wireIn Data input.
     * @return MethodReaderStatus.
     */
    protected MethodReaderStatus readOneGenerated(WireIn wireIn) {
        readOneCall(wireIn);
        return MethodReaderStatus.KNOWN;
    }

    /**
     * Reads call name and arguments from the wire and performs invocation on a target object instance.
     * Implementation of this method is generated in runtime, see {@link GenerateMethodReader}.
     *
     * @param wireIn Data input.
     * @return {@code true} read a known event, <code>false</code> if reading should be delegated.
     */
    @Deprecated(/* for removal in x.26*/)
    protected boolean readOneCall(WireIn wireIn) {
        // one of these methods must be overridden
        readOneGenerated(wireIn);
        return true;
    }

    protected MethodReaderStatus readOneMetaGenerated(WireIn wireIn) {
        readOneCallMeta(wireIn);
        return MethodReaderStatus.KNOWN;
    }

    @Deprecated(/* for removal in x.26*/)
    protected boolean readOneCallMeta(WireIn wireIn) {
        // one of these methods must be overridden
        readOneMetaGenerated(wireIn);
        return true;
    }

    /**
     * @param context Reading document context.
     * @return KNOWN, UNKNOWN, or EMPTY (no content)
     */
    public MethodReaderStatus readOne0(DocumentContext context) {
        WireIn wireIn = context.wire();
        if (wireIn == null)
            return MethodReaderStatus.EMPTY;

        if (historyConsumer != NO_OP_MH_CONSUMER) {
            writeUnwrittenMessageHistory(context);

            // Another reader may have swapped MessageHistory.get() and TEMP_MESSAGE_HISTORY
            // Clearing local reference to recover link to the proper thread-local, which is MessageHistory.get()
            messageHistory = null;
        }

        messageHistory().reset(context.sourceId(), context.index());

        try {
            wireIn.startEvent();
            wireIn.consumePadding();
            Bytes<?> bytes = wireIn.bytes();
            dataEventProcessed = false;
            MethodReaderStatus decoded = MethodReaderStatus.EMPTY; // no message
            while (bytes.readRemaining() > 0) {
                if (wireIn.isEndEvent())
                    break;
                long start = bytes.readPosition();

                MethodReaderStatus mrs = context.isData()
                        ? readOneGenerated(wireIn)
                        : readOneMetaGenerated(wireIn);
                switch (mrs) {
                    case HISTORY:
                        // unchanged
                        break;
                    case KNOWN:
                        decoded = MethodReaderStatus.KNOWN;
                        break;
                    case UNKNOWN:
                        if (decoded == MethodReaderStatus.EMPTY)
                            decoded = MethodReaderStatus.UNKNOWN;
                        break;
                    default:
                        throw new AssertionError(mrs);
                }

                if (restIgnored())
                    return decoded;

                wireIn.consumePadding();
                if (bytes.readPosition() == start) {
                    logNonProgressWarning(bytes.readRemaining());
                    return decoded;
                }
            }
            wireIn.endEvent();
            return decoded;

        } finally {
            // Don't save message history if we are reading non-data event (e.g. another "message history only" message)
            // Infinite loop between services is possible otherwise
            if (historyConsumer != NO_OP_MH_CONSUMER && dataEventProcessed)
                swapMessageHistoryIfDirty();
            messageHistory.reset();
        }
    }

    private void logNonProgressWarning(long bytes) {
        Jvm.warn().on(getClass(), "Failed to progress reading " + bytes + " bytes left.");
    }

    protected boolean restIgnored() {
        return false;
    }

    /**
     * uses a double buffer technique to swap the current message history with a temp message history ( this is, if it has not already been stored ) .
     */
    private void swapMessageHistoryIfDirty() {
        if (messageHistory.isDirty()) {
            // This input event didn't generate an output event.
            // Saving message history - in case next input event will be processed by another method reader,
            // that method reader will cooperatively write saved history.
            messageHistory = tempMessageHistory.getAndSet(messageHistory);
            MessageHistory.set(messageHistory);
            assert (messageHistory != tempMessageHistory.get());
        } else {
            // This input event generated an output event.
            // In case previous input event was processed by this method reader, TEMP_MESSAGE_HISTORY may contain
            // stale info on event's message history, which is superseded by the message history written now.
            tempMessageHistory.get().reset();
        }
    }

    /**
     * writes the history message for the last message ( if required ), that is, if the last input,
     * has not yet written its message history yet to the output queue.
     *
     * @param context the DocumentContext of the output queue that we are going to write the message history to
     */
    private void writeUnwrittenMessageHistory(DocumentContext context) {
        final MessageHistory mh = tempMessageHistory.get();
        if (mh.sources() != 0 && context.sourceId() != mh.lastSourceId() && mh.isDirty())
            historyConsumer.accept(mh);
    }

    @Override
    public boolean readOne() {
        if (!predicate.test(this))
            return false;

        do {
            throwExceptionIfClosed();

            try (DocumentContext context = in.readingDocument()) {
                if (!context.isPresent()) {
                    break;
                }

                MethodReaderStatus mrs = readOne0(context);
                switch (mrs) {
                    case KNOWN:
                        if (scanning && context.isMetaData())
                            break; // continue
                        return true;
                    case EMPTY:
                    case UNKNOWN:
                        if (scanning)
                            break; // continue looking
                        return true;
                    default:
                        throw new AssertionError(mrs);
                }
                // retry on a data message unless a known message is found.
            }
        } while (scanning);
        return false;
    }

    public void throwExceptionIfClosed() {
        if (isClosed())
            throw new IllegalStateException("Closed");
    }

    @Override
    public MethodReaderInterceptorReturns methodReaderInterceptorReturns() {
        return null;
    }

    @Override
    public void close() {
        if (closeIn)
            closeQuietly(in);
        closed = true;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public MethodReader closeIn(boolean closeIn) {
        throwExceptionIfClosed();
        this.closeIn = closeIn;
        return this;
    }

    /**
     * Offers a workaround to selectively disable "object recycling read" provided by {@link ValueIn#object(Object, Class)}
     * for specific object types. This ensures that certain objects, such as arrays and collections, are
     * not unintentionally reused or recycled during the reading process.
     *
     * @param <T> The generic type of the object to check.
     * @param o   The object instance to verify and possibly recycle.
     * @return The object itself if recycling is not applied, or {@code null} if the object is either
     * {@code null} or an array. If the object is a collection or map, the method will clear its
     * content and return the object.
     */
    protected <T> T checkRecycle(T o) {
        if (o == null || o.getClass().isArray()) // If the object is null or an array, return null to prevent recycling.
            return null;

        if (o instanceof Collection) { // If the object is a collection, clear its content.
            ((Collection<?>) o).clear();
        }

        if (o instanceof Map) { // If the object is a map, clear its content.
            ((Map<?, ?>) o).clear();
        }

        // For objects of type AbstractMarshallableCfg, reset them to their default state.
        if (o instanceof AbstractMarshallableCfg) {
            ((AbstractMarshallableCfg) o).reset();
        }

        // Return the potentially modified object.
        return o;
    }

    protected Object actualInvoke(Method method, Object o, Object[] objects) {
        try {
            return method.invoke(o, objects);
        } catch (Exception e) {
            throw Jvm.rethrow(e);
        }
    }

    private MessageHistory messageHistory() {
        if (messageHistory == null)
            messageHistory = MessageHistory.get();

        return messageHistory;
    }

    public void scanning(boolean scanning) {
        this.scanning = scanning;
    }

    private static final class MessageHistoryThreadLocal {

        private final ThreadLocal<MessageHistory> messageHistoryTL = withInitial(() -> {
            @NotNull VanillaMessageHistory veh = new VanillaMessageHistory();
            veh.addSourceDetails(true);
            return veh;
        });

        private MessageHistory getAndSet(MessageHistory mh) {
            final MessageHistory result = messageHistoryTL.get();
            messageHistoryTL.set(mh);
            return result;
        }

        public MessageHistory get() {
            return messageHistoryTL.get();
        }
    }
}
