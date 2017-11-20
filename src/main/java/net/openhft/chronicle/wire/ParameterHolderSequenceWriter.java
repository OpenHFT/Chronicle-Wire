package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.MethodId;
import net.openhft.chronicle.core.util.Annotations;

import java.lang.reflect.Method;
import java.util.function.BiConsumer;

// lambda was causing garbage
class ParameterHolderSequenceWriter {
    final Class[] parameterTypes;
    final BiConsumer<Object[], ValueOut> from0;
    final BiConsumer<Object[], ValueOut> from1;
    final long methodId;

    protected ParameterHolderSequenceWriter(Method method) {
        this.parameterTypes = method.getParameterTypes();
        this.from0 = (a, v) -> {
            for (int i = 0; i < parameterTypes.length; i++)
                v.object(parameterTypes[i], a[i]);
        };
        this.from1 = (a, v) -> {
            for (int i = 1; i < parameterTypes.length; i++)
                v.object(parameterTypes[i], a[i]);
        };
        MethodId methodId = Annotations.getAnnotation(method, MethodId.class);
        this.methodId = methodId == null ? Long.MIN_VALUE : methodId.value();
    }
}
