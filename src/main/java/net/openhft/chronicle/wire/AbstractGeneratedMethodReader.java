/*
 * Copyright 2016-2020 Chronicle Software
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
import net.openhft.chronicle.core.io.Closeable;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Base class for generated method readers.
 * In case generated instance fails to perform a read, it's delegated to lazy-initialized {@link VanillaMethodReader}.
 */
public abstract class AbstractGeneratedMethodReader implements MethodReader {
    private final MarshallableIn in;
    protected final WireParselet debugLoggingParselet;
    private final Supplier<MethodReader> delegateSupplier;

    protected MessageHistory messageHistory;
    private MethodReader delegate;
    private boolean closeIn = false, closed;

    public AbstractGeneratedMethodReader(MarshallableIn in,
                                         WireParselet debugLoggingParselet,
                                         Supplier<MethodReader> delegateSupplier) {
        this.in = in;
        this.debugLoggingParselet = debugLoggingParselet;
        this.delegateSupplier = delegateSupplier;
    }

    protected abstract boolean readOneCall(WireIn wireIn);

    /**
     * @param context Reading document context.
     * @return <code>true</code> if reading is successful, <code>false</code> if reading should be delegated.
     */
    public boolean readOne0(DocumentContext context) {
        if (context.isMetaData())
            return false;

        messageHistory().reset(context.sourceId(), context.index());

        WireIn wireIn = context.wire();

        if (wireIn == null)
            return false;

        wireIn.startEvent();
        Bytes<?> bytes = wireIn.bytes();
        while (bytes.readRemaining() > 0) {
            if (wireIn.isEndEvent())
                break;
            long start = bytes.readPosition();

            if (!readOneCall(wireIn))
                return false;

            wireIn.consumePadding();
            if (bytes.readPosition() == start) {
                Jvm.warn().on(getClass(), "Failed to progress reading " + bytes.readRemaining() + " bytes left.");
                break;
            }
        }
        wireIn.endEvent();

        return true;
    }

    @Override
    public boolean readOne() {
        throwExceptionIfClosed();

        boolean shouldDelegate;

        try (DocumentContext context = in.readingDocument()) {
            if (!context.isPresent()) {
                return false;
            }

            shouldDelegate = !readOne0(context);

            if (shouldDelegate)
                context.rollbackOnClose();
        }

        if (shouldDelegate)
            return delegate().readOne();
        else
            return true;
    }

    @Override
    public MethodReaderInterceptorReturns methodReaderInterceptorReturns() {
        return null;
    }

    @Override
    public void close() {
        if (closeIn)
            Closeable.closeQuietly(in);
        closed = true;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public boolean lazyReadOne() {
        throwExceptionIfClosed();

        if (!in.peekDocument()) {
            return false;
        }

        return readOne();
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

    private MessageHistory messageHistory() {
        if (messageHistory == null)
            messageHistory = MessageHistory.get();

        return messageHistory;
    }

    private MethodReader delegate() {
        if (delegate == null)
            delegate = delegateSupplier.get();

        return delegate;
    }
}
