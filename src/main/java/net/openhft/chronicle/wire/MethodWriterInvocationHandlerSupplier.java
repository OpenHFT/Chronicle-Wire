package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.MethodWriterInterceptor;
import net.openhft.chronicle.bytes.MethodWriterInterceptorReturns;
import net.openhft.chronicle.bytes.MethodWriterInvocationHandler;
import net.openhft.chronicle.bytes.MethodWriterListener;
import net.openhft.chronicle.core.io.Closeable;

import java.util.function.Supplier;

public class MethodWriterInvocationHandlerSupplier implements Supplier<MethodWriterInvocationHandler> {
    private final Supplier<MethodWriterInvocationHandler> supplier;
    private boolean recordHistory;
    private MethodWriterListener methodWriterListener;
    private MethodWriterInterceptorReturns methodWriterInterceptorReturns;
    private Closeable closeable;
    private boolean disableThreadSafe;
    private String genericEvent;
    private boolean useMethodIds = true;
    private final ThreadLocal<MethodWriterInvocationHandler> handlerTL = ThreadLocal.withInitial(() -> newHandler());
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
