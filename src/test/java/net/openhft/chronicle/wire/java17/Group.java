//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.java17;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;

/**
 * Represents a group that contains a field and its related configurations.
 * Inherits the capabilities of SelfDescribingMarshallable to provide
 * self-describing marshalling and unmarshalling.
 */
class Group extends SelfDescribingMarshallable {

    // The field associated with this group
    private Field field;

    /**
     * Constructs a Group with the specified field.
     *
     * @param field The field to be associated with this group.
     */
    public Group(Field field) {
        this.field = field;
    }
}
