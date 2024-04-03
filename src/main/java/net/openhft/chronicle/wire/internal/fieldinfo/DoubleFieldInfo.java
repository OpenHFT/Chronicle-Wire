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

package net.openhft.chronicle.wire.internal.fieldinfo;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.UnsafeMemory;
import net.openhft.chronicle.wire.BracketType;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

public final class DoubleFieldInfo extends UnsafeFieldInfo {

    public DoubleFieldInfo(String name, Class<?> type, BracketType bracketType, @NotNull Field field) {
        super(name, type, bracketType, field);
    }

    @Override
    public double getDouble(Object object) {
        try {
            return UnsafeMemory.unsafeGetDouble(object, getOffset());
        } catch (@NotNull NoSuchFieldException e) {
            Jvm.debug().on(DoubleFieldInfo.class, e);
            return Double.NaN;
        }
    }

    @Override
    public void set(Object object, double value) throws IllegalArgumentException {
        try {
            UnsafeMemory.unsafePutDouble(object, getOffset(), value);
        } catch (@NotNull NoSuchFieldException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public boolean isEqual(Object a, Object b) {
        return getDouble(a) == getDouble(b);
    }

    @Override
    public void copy(Object source, Object destination) {
        set(destination, getDouble(source));
    }
}
