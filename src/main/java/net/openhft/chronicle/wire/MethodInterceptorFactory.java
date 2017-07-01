package net.openhft.chronicle.wire;

import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * Created by peter on 01/07/17.
 */
public interface MethodInterceptorFactory {
    Consumer<Object[]> create(Method method);
}
