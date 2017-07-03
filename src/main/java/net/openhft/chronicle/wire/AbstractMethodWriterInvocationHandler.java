/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.core.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import static net.openhft.chronicle.wire.MethodReader.NO_ARGS;

/**
 * Created by peter on 25/03/16.
 */
public abstract class AbstractMethodWriterInvocationHandler implements MethodWriterInvocationHandler {
    private final Map<Method, Class[]> parameterMap = new ConcurrentHashMap<>();
    protected boolean recordHistory;
    private Closeable closeable;
    private Map<Method, Consumer<Object[]>> methodConsumerMap;
    private Function<Method, Consumer<Object[]>> methodFactoryLambda;

    // Note the Object[] passed in creates an object on every call.
    @Nullable
    @Override
    public Object invoke(Object proxy, @NotNull Method method, Object[] args) throws Throwable {
        Class<?> declaringClass = method.getDeclaringClass();
        if (declaringClass == Object.class) {
            return method.invoke(this, args);
        } else if (declaringClass == Closeable.class && method.getName().equals("close")) {
            Closeable.closeQuietly(closeable);
            return null;
        }
        if (args == null)
            args = NO_ARGS;
        if (methodFactoryLambda != null) {
            Consumer<Object[]> consumer = methodConsumerMap.computeIfAbsent(method, methodFactoryLambda);
            if (consumer != null)
                consumer.accept(args);
        }
        handleInvoke(method, args);
        return ObjectUtils.defaultValue(method.getReturnType());
    }

    protected abstract void handleInvoke(Method method, Object[] args);

    protected void handleInvoke(@NotNull Method method, Object[] args, Wire wire) {
        if (recordHistory) {
            wire.write("history").marshallable(MessageHistory.get());
        }
        ValueOut valueOut = wire
                .writeEventName(method.getName());
        Class[] parameterTypes = parameterMap.computeIfAbsent(method, Method::getParameterTypes);
        switch (parameterTypes.length) {
            case 0:
                valueOut.text("");
                break;
            case 1:
                valueOut.object(parameterTypes[0], args[0]);
                break;
            default:
                final Class[] finalParameterTypes = parameterTypes;
                valueOut.sequence(args, (a, v) -> {
                    for (int i = 0; i < finalParameterTypes.length; i++)
                        v.object(finalParameterTypes[i], a[i]);
                });
        }
    }

    public void recordHistory(boolean recordHistory) {
        this.recordHistory = recordHistory;
    }

    public void onClose(Closeable closeable) {
        this.closeable = closeable;
    }

    @Override
    public void methodInterceptorFactory(MethodInterceptorFactory methodInterceptorFactory) {
        methodConsumerMap = new LinkedHashMap<>();
        if (methodInterceptorFactory == null)
            methodFactoryLambda = null;
        else
            methodFactoryLambda = methodInterceptorFactory::create;
    }
}
