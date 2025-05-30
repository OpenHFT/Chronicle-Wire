/*
 * Copyright 2016-2025 chronicle.software
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
import net.openhft.chronicle.wire.internal.VanillaFieldInfo;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

/**
 * Base class for field access using {@link UnsafeMemory}. It resolves the
 * memory offset of the underlying field and stores it for direct operations.
 */
@SuppressWarnings("deprecation" /* The parent class will either be moved to internal or cease to exist in x.26 */)
class UnsafeFieldInfo extends VanillaFieldInfo {
    /** Offset value to indicate that it has not been set yet. */
    private static final long UNSET_OFFSET = Long.MAX_VALUE;

    /**
     * Memory offset of this field within its declaring class. Calculated on
     * first use and cached. Marked transient as it is JVM specific.
     */
    private transient long offset = UNSET_OFFSET;

    /**
     * Creates an instance linked to a particular field.
     *
     * @param name        textual field name
     * @param type        runtime type of the field
     * @param bracketType formatting hint used when writing
     * @param field       reflection field from which the offset will be derived
     */
    public UnsafeFieldInfo(String name, Class<?> type, BracketType bracketType, @NotNull Field field) {
        super(name, type, bracketType, field);
    }

    /**
     * Obtains the memory offset of this field using {@link UnsafeMemory} on the
     * first call and caches it for subsequent access.
     *
     * @return the memory offset for direct field operations
     * @throws NoSuchFieldException if {@link #getField()} fails
     */
    protected long getOffset() throws NoSuchFieldException {
        if (this.offset == UNSET_OFFSET) {
            offset = UnsafeMemory.unsafeObjectFieldOffset(getField());
        }
        return this.offset;
    }
}
