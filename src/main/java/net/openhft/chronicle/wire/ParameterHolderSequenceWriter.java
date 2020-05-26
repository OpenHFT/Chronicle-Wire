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

import net.openhft.chronicle.bytes.MethodId;
import net.openhft.chronicle.core.util.Annotations;

import java.lang.reflect.Method;
import java.util.function.BiConsumer;

// lambda was causing garbage
class ParameterHolderSequenceWriter {
    @SuppressWarnings("rawtypes")
    final Class[] parameterTypes;
    final BiConsumer<Object[], ValueOut> from0;
    final BiConsumer<Object[], ValueOut> from1;
    final long methodId;

    @SuppressWarnings("unchecked")
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
