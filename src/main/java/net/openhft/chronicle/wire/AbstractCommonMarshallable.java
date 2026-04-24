/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.BytesMarshallable;
import net.openhft.chronicle.core.io.ValidatableUtil;

/**
 * This uses bytes marshallable, non self describing messages by default.
 * use {@link SelfDescribingMarshallable} or {@link BytesInBinaryMarshallable} instead
 */
abstract class AbstractCommonMarshallable implements Marshallable, BytesMarshallable {
    @Override
    public boolean equals(Object o) {
        return Marshallable.$equals(this, o);
    }

    @Override
    public int hashCode() {
        return Marshallable.$hashCode(this);
    }

    @Override
    public String toString() {
        // this allows even invalid DTOs to be written to dump on a best-effort basis.
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
