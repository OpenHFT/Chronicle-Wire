/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.reordered;

import net.openhft.chronicle.wire.*;
import net.openhft.chronicle.wire.reuse.AbstractPooledOuterClass;

/**
 * OuterClass extends AbstractPooledOuterClass to facilitate serialization and deserialization.
 * It contains multiple lists of NestedClass objects and handles custom serialization logic.
 */
@SuppressWarnings({"deprecation", "removal"})
class OuterClass extends AbstractPooledOuterClass<NestedClass> {

    public OuterClass() {
        super(NestedClass::new);
    }
}
