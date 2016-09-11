/*
 * Copyright 2016 higherfrequencytrading.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.openhft.chronicle.wire;

/**
 * Created by peter on 10/05/16.
 */
public interface SerializationStrategy<T> {
    default T read(ValueIn in, Class<T> type) {
        return readUsing(newInstance(type), in);
    }

    default T readUsing(T using, ValueIn in, Class<T> type) {
        if (using == null && type != null)
            using = newInstance(type);
        return readUsing(using, in);
    }

    T readUsing(T using, ValueIn in);

    T newInstance(Class<T> type);

    Class<T> type();

    BracketType bracketType();
}
