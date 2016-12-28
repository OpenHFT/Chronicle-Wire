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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by peter on 25/03/16.
 */
public class MethodWriterInvocationHandler implements InvocationHandler {
    @NotNull
    private final MarshallableOut appender;
    private final Map<Method, Class[]> parameterMap = new ConcurrentHashMap<>();
    private boolean recordHistory;
    private Closeable closeable;

    MethodWriterInvocationHandler(@NotNull MarshallableOut appender) {
        this.appender = appender;
        recordHistory = appender.recordHistory();
    }

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
        try (@NotNull DocumentContext context = appender.writingDocument()) {
            Wire wire = context.wire();

            if (recordHistory) {
                wire.write("history").marshallable(MessageHistory.get());
            }
            ValueOut valueOut = wire
                    .writeEventName(method.getName());
            Class[] parameterTypes = parameterMap.get(method);
            if (parameterTypes == null)
                parameterMap.put(method, parameterTypes = method.getParameterTypes());
            switch (parameterTypes.length) {
                case 0:
                    valueOut.text("");
                    break;
                case 1:
                    valueOut.object(parameterTypes[0], args[0]);
                    break;
                default:
                    final Class[] finalParameterTypes = parameterTypes;
                    valueOut.sequence(v -> {
                        for (int i = 0; i < finalParameterTypes.length; i++)
                            v.object(finalParameterTypes[i], args[i]);
                    });
            }
            wire.padToCacheAlign();
        }
        return ObjectUtils.defaultValue(method.getReturnType());
    }

    public void recordHistory(boolean recordHistory) {
        this.recordHistory = recordHistory;
    }

    public void onClose(Closeable closeable) {
        this.closeable = closeable;
    }
}
