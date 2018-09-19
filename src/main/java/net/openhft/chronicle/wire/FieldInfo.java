/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire;

import org.jetbrains.annotations.Nullable;

/*
 * Created by Peter Lawrey on 18/10/16.
 */
public interface FieldInfo {

    String name();

    Class type();

    BracketType bracketType();

    @Nullable
    Object get(Object object);

    long getLong(Object object);

    int getInt(Object object);

    char getChar(Object object);

    double getDouble(Object object);

    void set(Object object, Object value) throws IllegalArgumentException;

    void set(Object object, char value) throws IllegalArgumentException;

    void set(Object object, int value) throws IllegalArgumentException;

    void set(Object object, long value) throws IllegalArgumentException;

    void set(Object object, double value) throws IllegalArgumentException;

    Class genericType(int index);
}
