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
 * This is similar to ReadMarshallable however it is expected that
 * a new potentially immutable object will be created each time.
 * <p>
 * Any implementation must have a constructor which takes a WireIn for deserialization.
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
