package net.openhft.chronicle.wire;

import java.lang.reflect.Method;
import java.util.function.BiConsumer;

/*
 * Created by Jerry Shea 25/10/2017
 * <p>
 * Invoked around method writing allowing you to take action before or after method invocation,
 * or even not to call the method
 */
@FunctionalInterface
public interface MethodWriterInterceptor {

    void intercept(Method method, Object[] args, BiConsumer<Method, Object[]> invoker);

    static MethodWriterInterceptor of(final MethodWriterListener methodWriterListener) {
        if (methodWriterListener == null)
            return null;
        return (method, args, invoker) -> {
            methodWriterListener.onWrite(method.getName(), args);
            invoker.accept(method, args);
        };
    }
}
