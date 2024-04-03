/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
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

package net.openhft.chronicle.wire.internal;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.stream.Stream;

public enum GenericReflection {
    ;

    public static Type getReturnType(Method m, Class<?>forClass) {
        final Type genericReturnType = m.getGenericReturnType();
        if (genericReturnType instanceof Class)
            return genericReturnType;
        final Class<?> declaringClass = m.getDeclaringClass();
        final Optional<? extends Type> extendsType = Stream.of(
                        Stream.of(forClass.getGenericSuperclass()), Stream.of(forClass.getGenericInterfaces()))
                .flatMap(s -> s)
                .filter(t -> declaringClass.equals(erase(t)))
                .findFirst();
        final Type[] typeParameters = declaringClass.getTypeParameters();
        if (extendsType.isPresent() && extendsType.get() instanceof ParameterizedType) {
            final ParameterizedType type = (ParameterizedType) extendsType.get();
            final Type[] actualTypeArguments = type.getActualTypeArguments();
            for (int i = 0; i < typeParameters.length; i++)
                if (typeParameters[i].equals(genericReturnType))
                    return actualTypeArguments[i];
        }
        return m.getReturnType();
    }

    private static Class<?> erase(Type t) {
        return t instanceof ParameterizedType
                ? erase(((ParameterizedType) t).getRawType())
                : (Class<?>) t;
    }
}
