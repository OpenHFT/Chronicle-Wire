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

import net.openhft.chronicle.bytes.MethodWriterInterceptor;
import net.openhft.chronicle.bytes.MethodWriterInterceptorReturns;
import net.openhft.chronicle.bytes.MethodWriterInvocationHandler;
import net.openhft.chronicle.bytes.MethodWriterListener;
import net.openhft.chronicle.core.io.Closeable;

import java.util.function.Function;
import java.util.function.Supplier;

public class MethodWriterInvocationHandlerSupplier<T> implements Supplier<MethodWriterInvocationHandler> {
    private final Supplier<MethodWriterInvocationHandler> supplier;
    private Function<T,T> modifier;
    private boolean recordHistory;
    private MethodWriterListener methodWriterListener;
    private MethodWriterInterceptorReturns methodWriterInterceptorReturns;
    private Closeable closeable;
    private boolean disableThreadSafe;
    private String genericEvent;
    private boolean useMethodIds = true;
    private final ThreadLocal<MethodWriterInvocationHandler> handlerTL = ThreadLocal.withInitial(this::newHandler);
    private MethodWriterInvocationHandler handler;

    public MethodWriterInvocationHandlerSupplier(Supplier<MethodWriterInvocationHandler> supplier) {
        this.supplier = supplier;
    }

    public void recordHistory(boolean recordHistory) {
        this.recordHistory = recordHistory;
    }

    public void methodWriterListener(MethodWriterListener methodWriterListener) {
        this.methodWriterListener = methodWriterListener;
    }

    public void methodWriterInterceptor(MethodWriterInterceptor methodWriterInterceptor) {
        this.methodWriterInterceptorReturns = MethodWriterInterceptorReturns.of(methodWriterInterceptor);
    }

    public MethodWriterInvocationHandlerSupplier methodWriterInterceptorReturns(MethodWriterInterceptorReturns methodWriterInterceptorReturns) {
        this.methodWriterInterceptorReturns = methodWriterInterceptorReturns;
        return this;
    }

    public MethodWriterInterceptorReturns methodWriterInterceptorReturns() {
        return methodWriterInterceptorReturns;
    }

    public void onClose(Closeable closeable) {
        this.closeable = closeable;
    }

    public void disableThreadSafe(boolean disableThreadSafe) {
        this.disableThreadSafe = disableThreadSafe;
    }

    public void genericEvent(String genericEvent) {
        this.genericEvent = genericEvent;
    }

    public void useMethodIds(boolean useMethodIds) {
        this.useMethodIds = useMethodIds;
    }

    public MethodWriterInvocationHandlerSupplier modifier(final Function<T, T> modifier) {
        this.modifier = modifier;
        return this;
    }

    private MethodWriterInvocationHandler newHandler() {
        MethodWriterInvocationHandler h = supplier.get();
        h.genericEvent(genericEvent);
        h.methodWriterInterceptorReturns(methodWriterListener, methodWriterInterceptorReturns);
        h.onClose(closeable);
        h.recordHistory(recordHistory);
        h.useMethodIds(useMethodIds);
        return h;
    }

    @Override
    public MethodWriterInvocationHandler get() {
        if (disableThreadSafe) {
            if (handler == null) {
                handler = newHandler();
            }
            return handler;
        }
        return handlerTL.get();
    }
}
