/*
 *
 *  *     Copyright (C) 2016  higherfrequencytrading.com
 *  *
 *  *     This program is free software: you can redistribute it and/or modify
 *  *     it under the terms of the GNU Lesser General Public License as published by
 *  *     the Free Software Foundation, either version 3 of the License.
 *  *
 *  *     This program is distributed in the hope that it will be useful,
 *  *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  *     GNU Lesser General Public License for more details.
 *  *
 *  *     You should have received a copy of the GNU Lesser General Public License
 *  *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * This is similar to ReadMarshallable however it is expected that
 * a new potentially immutable object will be created each time.
 * <p/>
 * Any implementation must have a constructor which takes a WireIn for deserialization.
 */
public interface Demarshallable {
    // Demarshallable(WireIn)

    ClassValue<Constructor<Demarshallable>> DEMARSHALLABLES = new ClassValue<Constructor<Demarshallable>>() {
        @Override
        protected Constructor<Demarshallable> computeValue(Class<?> type) {
            try {
                Constructor<Demarshallable> declaredConstructor =
                        (Constructor<Demarshallable>)
                                type.getDeclaredConstructor(WireIn.class);
                declaredConstructor.setAccessible(true);
                return declaredConstructor;
            } catch (NoSuchMethodException e) {
                throw new AssertionError(e);
            }
        }
    };

    @NotNull
    static <T extends Demarshallable> T newInstance(Class<T> clazz, WireIn wireIn) {
        try {
            return (T) DEMARSHALLABLES.get(clazz).newInstance(wireIn);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        } catch (Throwable e) {
            if (e instanceof InvocationTargetException)
                e = e.getCause();
            throw new IORuntimeException(e);
        }
    }
}
