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
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.BytesMarshallable;
import net.openhft.chronicle.core.io.ValidatableUtil;

/**
 * Base class for marshallable objects backed by
 * {@link net.openhft.chronicle.bytes.BytesMarshallable}.  By default it assumes
 * the wire format is not self describing.  For self describing objects use
 * {@link SelfDescribingMarshallable}.  For raw bytes handling consider
 * {@link BytesInBinaryMarshallable}.
 */
abstract class AbstractCommonMarshallable implements Marshallable, BytesMarshallable {
    @Override
    /** Delegates to {@link Marshallable#$equals(WriteMarshallable, Object)}. */
    public boolean equals(Object o) {
        return Marshallable.$equals(this, o);
    }

    @Override
    /** Delegates to {@link Marshallable#$hashCode(WriteMarshallable)}. */
    public int hashCode() {
        return Marshallable.$hashCode(this);
    }

    @Override
    /**
     * Serialises this object to text for debugging, temporarily disabling
     * validation.
     */
    public String toString() {
        // this allows even invalid DTOs to be written to dump on a best effort basis.
        ValidatableUtil.startValidateDisabled();
        try {
            return Marshallable.$toString(this);
        } catch (Throwable e) {
            return getClass() + "  " + e;
        } finally {
            ValidatableUtil.endValidateDisabled();
        }
    }
}
