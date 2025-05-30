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
 * Contract for classes that are created afresh from a {@link WireIn} rather than
 * having an existing instance reused.  This is particularly suited to immutable
 * objects.  Implementations must provide a constructor taking a {@code WireIn}
 * which is used by {@link #newInstance(Class, WireIn)} to create and populate a
 * new instance.
 */
public interface Demarshallable {

    /**
     * Cache of constructors taking a single {@link WireIn} argument for each
     * {@code Demarshallable} implementation.  Using a {@link ClassValue}
     * avoids repeated reflective lookups when creating new instances.
     */
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
     * Utility method that uses the cached constructor to create a new instance
     * of {@code clazz} and immediately populates it from {@code wireIn}.
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
