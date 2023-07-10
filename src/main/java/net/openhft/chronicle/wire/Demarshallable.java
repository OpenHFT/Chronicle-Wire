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
 * An interface representing an object that can be demarshalled
 * from a WireIn instance. Each demarshalling operation is expected
 * to create a new, potentially immutable, object.
 *
 * Implementations of this interface must have a constructor
 * that takes a WireIn instance as a parameter for deserialization.
 */
public interface Demarshallable {

    ClassValue<Constructor<Demarshallable>> DEMARSHALLABLES = new ClassValue<Constructor<Demarshallable>>() {
        @NotNull
        @Override
        protected Constructor<Demarshallable> computeValue(@NotNull Class<?> type) {
            try {
                @SuppressWarnings("unchecked")
                @NotNull Constructor<Demarshallable> declaredConstructor =
                        (Constructor<Demarshallable>)
                                type.getDeclaredConstructor(WireIn.class);
                Jvm.setAccessible(declaredConstructor);
                return declaredConstructor;
            } catch (NoSuchMethodException e) {
                throw new AssertionError(e);
            }
        }
    };

    @SuppressWarnings("unchecked")
    @NotNull
    /**
     * Creates a new instance of a class implementing the Demarshallable interface
     * using the provided WireIn for initialization.
     *
     * @param clazz the class of which an instance should be created.
     * @param wireIn the WireIn instance used for initialization.
     * @return a new instance of the specified class.
     * @throws AssertionError if an instance could not be created due to access restrictions.
     * @throws IORuntimeException if the underlying constructor throws an exception.
     */
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
