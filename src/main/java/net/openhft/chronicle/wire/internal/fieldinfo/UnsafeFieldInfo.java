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

import net.openhft.chronicle.core.UnsafeMemory;
import net.openhft.chronicle.wire.BracketType;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

@SuppressWarnings("deprecation" /* The parent class will either be moved to internal or cease to exist in x.26 */)
class UnsafeFieldInfo extends VanillaFieldInfo {
    // Offset value to indicate that it hasn't been set yet.
    private static final long UNSET_OFFSET = Long.MAX_VALUE;

    // Offset in memory where the value for this field can be found.
    private transient long offset = UNSET_OFFSET;

    /**
     * Constructs an instance of UnsafeFieldInfo with the provided details about a field.
     *
     * @param name        The name of the field.
     * @param type        The type of the field.
     * @param bracketType The bracket type associated with the field.
     * @param field       The actual field representation.
     */
    public UnsafeFieldInfo(String name, Class type, BracketType bracketType, @NotNull Field field) {
        super(name, type, bracketType, field);
    }

    /**
     * Retrieves the memory offset where the value for this field is stored.
     * If the offset hasn't been retrieved before, it leverages unsafe operations
     * to get it and then caches the result.
     *
     * @return The memory offset for this field.
     * @throws NoSuchFieldException if there's a field access issue.
     */
    protected long getOffset() throws NoSuchFieldException {
        if (this.offset == UNSET_OFFSET) {
            offset = UnsafeMemory.unsafeObjectFieldOffset(getField());
        }
        return this.offset;
    }
}
