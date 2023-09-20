/*
 * Copyright 2016-2020 chronicle.software
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
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Represents a contract for objects that are designed for deserialization, with an expectation
 * that a new, potentially immutable object is instantiated during each deserialization process.
 * <p>
 * Unlike the `ReadMarshallable` pattern, the `Demarshallable` interface mandates that
 * implementing classes provide a constructor taking a `WireIn` instance to enable deserialization.
 * This approach ensures a clear mechanism to obtain a new object instance from the serialized data.
 * The interface also provides a utility to instantiate objects of implementing classes using the
 * appropriate constructor.
 *
 * @since 2023-09-14
 */
public interface Demarshallable {

    // Holds a cache for constructors of Demarshallable implementing classes, optimized for retrieval performance.
    ClassValue<Constructor<Demarshallable>> DEMARSHALLABLES = new ClassValue<Constructor<Demarshallable>>() {
        @NotNull
        @Override
        // Computes and returns the appropriate constructor for a given class type.
        protected Constructor<Demarshallable> computeValue(@NotNull Class<?> type) {
            try {
                @SuppressWarnings("unchecked")
                @NotNull Constructor<Demarshallable> declaredConstructor =
                        (Constructor<Demarshallable>)
                                type.getDeclaredConstructor(WireIn.class);
                // Ensure the constructor is accessible, even if it's a private constructor.
                Jvm.setAccessible(declaredConstructor);
                return declaredConstructor;
            } catch (NoSuchMethodException e) {
                throw new AssertionError(e);
            }
        }
    };

    /**
     * Provides a utility method to create a new instance of a class that implements the `Demarshallable` interface.
     * This method relies on the appropriate constructor from the cached constructors for efficient instantiation.
     *
     * @param clazz   The class type to be instantiated.
     * @param wireIn  The `WireIn` parameter to be passed to the constructor for deserialization.
     * @param <T>     The type of the object to be returned, which should implement `Demarshallable`.
     * @return        A new instance of the specified class type.
     */
    @SuppressWarnings("unchecked")
    @NotNull
    static <T extends Demarshallable> T newInstance(@NotNull Class<T> clazz, WireIn wireIn) {
        try {
            Constructor<Demarshallable> constructor = DEMARSHALLABLES.get(clazz);
            return (T) constructor.newInstance(wireIn);

        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        } catch (InvocationTargetException ite) {
            throw new IORuntimeException(ite.getCause());
        }  catch (Throwable e) {
            throw new IORuntimeException(e);
        }
    }
}
