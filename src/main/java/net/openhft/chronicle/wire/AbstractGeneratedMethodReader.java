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
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
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
 * This is the AbstractGeneratedMethodReader class implementing the MethodReader interface.
 * It serves as a base class for generated method readers, providing foundational functionality
 * and utility methods to facilitate method reading.
 */
@SuppressWarnings("deprecation")
public abstract class AbstractGeneratedMethodReader implements MethodReader {

    // A no-operation message history consumer.
    private static final Consumer<MessageHistory> NO_OP_MH_CONSUMER = Jvm.uncheckedCast(Mocker.ignored(Consumer.class));
    public final static ThreadLocal<String> SERVICE_NAME = new ThreadLocal<>();
    private final static ConcurrentHashMap<String, MessageHistoryThreadLocal> TEMP_MESSAGE_HISTORY_BY_SERVICE_NAME = new ConcurrentHashMap<>();
    protected final WireParselet debugLoggingParselet;
    private final MarshallableIn in;
    private final MessageHistoryThreadLocal tempMessageHistory;
    protected MessageHistory messageHistory;
    protected boolean dataEventProcessed;

    // Flag to determine if the input should be closed.
    private boolean closeIn = false;
    // Flag to determine if the reader is closed.
    private boolean closed;

    // Consumer for processing message history.
    private Consumer<MessageHistory> historyConsumer = NO_OP_MH_CONSUMER;

    private Predicate<MethodReader> predicate;
    private boolean scanning;

    /**
     * Constructs a new AbstractGeneratedMethodReader with the provided input interface
     * and a debug logging parselet.
     *
     * @param in                    The input interface for marshallable data
     * @param debugLoggingParselet  The parselet used for debugging and logging
     */
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

    /**
     * Sets a predicate to be used by the method reader.
     *
     * @param predicate The predicate for filtering
     * @return The current instance of the AbstractGeneratedMethodReader class
     */
    public AbstractGeneratedMethodReader predicate(Predicate<MethodReader> predicate) {
        this.predicate = predicate;
        return this;
    }

    /**
     * A utility method that assists implementations in retrieving a method from a class.
     * It looks up the method by its name and parameter types, making it accessible if it's private or protected.
     *
     * @param clazz          The class containing the method
     * @param name           The name of the method
     * @param parameterTypes The parameter types of the method
     * @return The method if found, otherwise throws an AssertionError
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
     * Sets a consumer for processing message history.
     * This consumer is invoked if the next message is for a different queue and the history
     * message isn't written to the output queue. It ensures that the LAST_WRITTEN mechanism
     * works even when no output is present for a given message.
     *
     * @param historyConsumer The consumer for processing message history
     */
    public void historyConsumer(Consumer<MessageHistory> historyConsumer) {
        this.historyConsumer = historyConsumer;
    }

    /**
     * Reads call name and arguments from the wire and performs invocation on a target object instance.
     * The implementation of this method is generated at runtime, see {@link GenerateMethodReader}.
     *
     * @param wireIn Data input.
     * @return MethodReaderStatus.
     */
    protected abstract MethodReaderStatus readOneGenerated(WireIn wireIn);

    protected abstract MethodReaderStatus readOneMetaGenerated(WireIn wireIn);

    /**
     * Reads the content based on the provided document context.
     *
     * @param context Reading document context.
     * @return KNOWN if the read event is known, UNKNOWN if the event is not recognized, or EMPTY if no content is present.
     */
    public MethodReaderStatus readOne0(DocumentContext context) {
        WireIn wireIn = context.wire();

        // Return EMPTY status if no content.
        if (wireIn == null)
            return MethodReaderStatus.EMPTY;

        // Check if we need to write the unwritten message history.
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
            MethodReaderStatus decoded = MethodReaderStatus.EMPTY; // Initialize status as no message.

            // Read and process all remaining bytes.
            while (bytes.readRemaining() > 0) {
                if (wireIn.isEndEvent())
                    break;
                long start = bytes.readPosition();

                // Read the wire based on whether it's data or metadata.
                MethodReaderStatus mrs = context.isData()
                        ? readOneGenerated(wireIn)
                        : readOneMetaGenerated(wireIn);

                // Update the decoding status based on the current read status.
                switch (mrs) {
                    case HISTORY:
                        // Status remains unchanged.
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

                // If any bytes are ignored, return the decoding status.
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

    /**
     * Logs a warning message indicating that there has been no progress in reading the wire data.
     * The warning provides information about the number of bytes that are left unread.
     *
     * @param bytes The number of bytes that are left unread.
     */
    private void logNonProgressWarning(long bytes) {
        Jvm.warn().on(getClass(), "Failed to progress reading " + bytes + " bytes left.");
    }

    /**
     * Determines if the rest of the wire data should be ignored.
     *
     * @return {@code true} if the rest should be ignored, {@code false} otherwise. Default implementation returns false.
     */
    protected boolean restIgnored() {
        return false;
    }

    /**
     * Swaps the current message history with a temporary message history using a double buffer technique.
     * The method ensures that if an input event did not generate an output event, its message history
     * will be saved, potentially to be written by another method reader in the future.
     * If an input event did generate an output, stale information from a previous input event is cleared.
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
     * Writes the message history of the last message to the output queue, but only if required.
     * This is determined by checking if the message history has not yet been written to the output queue.
     *
     * @param context The {@link DocumentContext} of the output queue where the message history might be written.
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

    /**
     * Checks if the method reader instance has been closed and throws a {@link ClosedIllegalStateException} if it is.
     * This method is intended to guard against operations on a closed instance, ensuring that no further
     * operations can be performed once the instance has been closed. This is particularly useful for
     * methods that modify the state of the instance or rely on its open state to function correctly.
     *
     * @throws ClosedIllegalStateException if this instance has already been closed.
     */
    public void throwExceptionIfClosed() throws ClosedIllegalStateException {
        if (isClosed())
            throw new ClosedIllegalStateException("Closed");
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

    /**
     * Invokes a given method on the provided object with the specified arguments.
     *
     * @param method The method to be invoked.
     * @param o The target object on which the method is to be invoked.
     * @param objects The arguments to the method.
     * @return Returns the result of the method invocation.
     * @throws RuntimeException if the method invocation throws an exception.
     */
    protected Object actualInvoke(Method method, Object o, Object[] objects) {
        try {
            return method.invoke(o, objects);
        } catch (Exception e) {
            throw Jvm.rethrow(e);
        }
    }

    /**
     * Retrieves the current message history.
     * If the message history is not initialized, fetches it from the MessageHistory class.
     *
     * @return The current message history.
     */
    private MessageHistory messageHistory() {
        if (messageHistory == null)
            messageHistory = MessageHistory.get();

        return messageHistory;
    }

    /**
     * Sets the scanning state.
     *
     * @param scanning The desired state of scanning. True to indicate scanning, false otherwise.
     */
    public void scanning(boolean scanning) {
        this.scanning = scanning;
    }

    /**
     * This is a helper class that manages thread-local instances of MessageHistory.
     * It provides methods to get and set the current thread's message history using a {@link ThreadLocal}.
     */
    private static final class MessageHistoryThreadLocal {

        private final ThreadLocal<MessageHistory> messageHistoryTL = withInitial(() -> {
            @NotNull VanillaMessageHistory veh = new VanillaMessageHistory();
            veh.addSourceDetails(true);
            return veh;
        });

        /**
         * Replaces the current thread's message history with the provided one
         * and returns the replaced message history.
         *
         * @param mh The new message history to set for the current thread.
         * @return The replaced message history.
         */
        private MessageHistory getAndSet(MessageHistory mh) {
            final MessageHistory result = messageHistoryTL.get();
            messageHistoryTL.set(mh);
            return result;
        }

        /**
         * Retrieves the current thread's message history.
         *
         * @return The current thread's message history.
         */
        public MessageHistory get() {
            return messageHistoryTL.get();
        }
    }
}
