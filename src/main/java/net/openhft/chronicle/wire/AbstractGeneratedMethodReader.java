/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
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
import net.openhft.chronicle.core.Mocker;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

import static java.lang.ThreadLocal.withInitial;
import static net.openhft.chronicle.core.io.Closeable.closeQuietly;

/**
 * Base class for generated method readers.
 */
public abstract class AbstractGeneratedMethodReader implements MethodReader {
    private static final Consumer<MessageHistory> NO_OP_MH_CONSUMER = Mocker.ignored(Consumer.class);
    private final MarshallableIn in;
    protected final WireParselet debugLoggingParselet;

    protected MessageHistory messageHistory;
    protected boolean dataEventProcessed;

    private MethodReader delegate;
    private boolean closeIn = false;
    private boolean closed;

    private Consumer<MessageHistory> historyConsumer = NO_OP_MH_CONSUMER;

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

    private static final MessageHistoryThreadLocal TEMP_MESSAGE_HISTORY = new MessageHistoryThreadLocal();

    protected AbstractGeneratedMethodReader(MarshallableIn in,
                                            WireParselet debugLoggingParselet) {
        this.in = in;
        this.debugLoggingParselet = debugLoggingParselet;
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
     * @return <code>true</code> if reading is successful, <code>false</code> if reading should be delegated.
     */
    protected abstract boolean readOneCall(WireIn wireIn);

    protected boolean readOneCallMeta(WireIn wireIn) {
        return false;
    }

    /**
     * @param context Reading document context.
     * @return <code>true</code> if reading is successful, <code>false</code> if reading should be delegated.
     */
    public boolean readOne0(DocumentContext context) {
        if (context.isMetaData())
            return false;

        WireIn wireIn = context.wire();
        if (wireIn == null)
            return false;

        if (historyConsumer != NO_OP_MH_CONSUMER) {
            writeUnwrittenMessageHistory(context);

            // Another reader may have swapped MessageHistory.get() and TEMP_MESSAGE_HISTORY
            // Clearing local reference to recover link to the proper thread-local, which is MessageHistory.get()
            messageHistory = null;
        }

        messageHistory().reset(context.sourceId(), context.index());

        try {
            wireIn.startEvent();
            Bytes<?> bytes = wireIn.bytes();
            dataEventProcessed = false;
            while (bytes.readRemaining() > 0) {
                if (wireIn.isEndEvent())
                    break;
                long start = bytes.readPosition();

                if (!readOneCall(wireIn))
                    return false;

                if (restIgnored())
                    return true;

                wireIn.consumePadding();
                if (bytes.readPosition() == start) {
                    Jvm.warn().on(getClass(), "Failed to progress reading " + bytes.readRemaining() + " bytes left.");
                    break;
                }
            }
            // only called if the end of the message is reached normally.
            wireIn.endEvent();
        } finally {
            // Don't save message history if we are reading non-data event (e.g. another "message history only" message)
            // Infinite loop between services is possible otherwise
            if (historyConsumer != NO_OP_MH_CONSUMER && dataEventProcessed)
                swapMessageHistoryIfDirty();
            messageHistory.reset();
        }

        return true;
    }

    public boolean readOneMeta(DocumentContext context) {
        WireIn wireIn = context.wire();
        if (wireIn == null)
            return false;

        wireIn.startEvent();
        Bytes<?> bytes = wireIn.bytes();
        while (bytes.readRemaining() > 0) {
            if (wireIn.isEndEvent())
                break;
            long start = bytes.readPosition();

            if (!readOneCallMeta(wireIn))
                return false;

            if (restIgnored())
                return true;

            wireIn.consumePadding();
            if (bytes.readPosition() == start) {
                Jvm.warn().on(getClass(), "Failed to progress reading " + bytes.readRemaining() + " bytes left.");
                break;
            }
        }
        // only called if the end of the message is reached normally.
        wireIn.endEvent();

        return true;
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
            messageHistory = TEMP_MESSAGE_HISTORY.getAndSet(messageHistory);
            MessageHistory.set(messageHistory);
            assert (messageHistory != TEMP_MESSAGE_HISTORY.get());
        } else {
            // This input event generated an output event.
            // In case previous input event was processed by this method reader, TEMP_MESSAGE_HISTORY may contain
            // stale info on event's message history, which is superseded by the message history written now.
            TEMP_MESSAGE_HISTORY.get().reset();
        }
    }

    /**
     * writes the history message for the last message ( if required ), that is, if the last input,
     * has not yet written its message history yet to the output queue.
     *
     * @param context the DocumentContext of the output queue that we are going to write the message history to
     */
    private void writeUnwrittenMessageHistory(DocumentContext context) {
        final MessageHistory mh = TEMP_MESSAGE_HISTORY.get();
        if (mh.sources() != 0 && context.sourceId() != mh.lastSourceId() && mh.isDirty())
            historyConsumer.accept(mh);
    }

    @Override
    public boolean readOne() {
        throwExceptionIfClosed();

        boolean ok;

        try (DocumentContext context = in.readingDocument()) {
            if (!context.isPresent()) {
                return false;
            }

            ok = context.isMetaData()
                    ? readOneMeta(context)
                    : readOne0(context);

            if (!ok)
                context.rollbackOnClose();
        }

        return ok;
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
     * Workaround to disable "object recycling read" {@link ValueIn#object(Object, Class)} for some object types.
     */
    protected <T> T checkRecycle(T o) {
        if (o == null || o.getClass().isArray()) // Arrays are kept intact by default.
            return null;

        if (o instanceof Collection) // Collections are not cleaned by default.
            ((Collection<?>) o).clear();

        if (o instanceof Map) // Maps are not cleaned by default.
            ((Map<?, ?>) o).clear();

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
}
