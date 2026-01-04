/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.serializable;

import net.openhft.chronicle.core.annotation.UsedViaReflection;
import java.io.Serializable;

@SuppressWarnings({"rawtypes","deprecation"})
@UsedViaReflection
public class SerializableScalarValues extends ScalarValues implements Serializable {
    private static final long serialVersionUID = 0L;

    public static SerializableScalarValues fromMarshallable(int i) {
        return new SerializableScalarValues(i);
    }

    public SerializableScalarValues() {
        super();
    }

    public SerializableScalarValues(int i) {
        super(i);
    }

}
