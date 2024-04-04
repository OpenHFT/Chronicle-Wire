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

/**
 * The GenericReflection utility class offers methods to extract and process type information
 * from Java's reflection API, especially focusing on generic types.
 * The enum design ensures utility behavior by not allowing instantiation.
 */
public enum GenericReflection {
    ;  // Ensures no instances of the enum

    /**
     * Retrieves the actual return type of a method when used in the context of a specific class.
     * If the method has a generic return type, this method attempts to resolve it based on the class's type parameters.
     *
     * @param m        The method for which the return type needs to be determined
     * @param forClass The class context within which the method's return type is to be evaluated
     * @return The resolved return type. It returns the raw type if generic resolution is not possible.
     */
    public static Type getReturnType(Method m, Class<?> forClass) {
        final Type genericReturnType = m.getGenericReturnType();

        // If the return type is not generic, return it
        if (genericReturnType instanceof Class)
            return genericReturnType;

        // Fetches the declaring class of the method
        final Class<?> declaringClass = m.getDeclaringClass();

        // Streams the generic superclass and interfaces of the provided class to find a match with the declaring class
        final Optional<? extends Type> extendsType = Stream.of(
                        Stream.of(forClass.getGenericSuperclass()), Stream.of(forClass.getGenericInterfaces()))
                .flatMap(s -> s)
                .filter(t -> declaringClass.equals(erase(t)))
                .findFirst();

        // Process to match type parameters and resolve generic return type
        final Type[] typeParameters = declaringClass.getTypeParameters();
        if (extendsType.isPresent() && extendsType.get() instanceof ParameterizedType) {
            final ParameterizedType type = (ParameterizedType) extendsType.get();
            final Type[] actualTypeArguments = type.getActualTypeArguments();
            for (int i = 0; i < typeParameters.length; i++)
                if (typeParameters[i].equals(genericReturnType))
                    return actualTypeArguments[i];
        }

        // Returns raw type if generic resolution is unsuccessful
        return m.getReturnType();
    }

    /**
     * Extracts the raw type from a provided Type. If the type is parameterized,
     * it returns the raw type of that parameterized type. Otherwise, returns the type casted as Class.
     *
     * @param t The type to be erased
     * @return The raw class type derived from the provided type
     */
    private static Class<?> erase(Type t) {
        // Checks if the type is parameterized and retrieves its raw type
        return t instanceof ParameterizedType
                ? erase(((ParameterizedType) t).getRawType())
                : (Class<?>) t;
    }
}
