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

package net.openhft.chronicle.wire.java17;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;

/**
 * Represents a group that contains a field and its related configurations.
 * Inherits the capabilities of SelfDescribingMarshallable to provide
 * self-describing marshalling and unmarshalling.
 */
public class Group extends SelfDescribingMarshallable {

    // The field associated with this group
    private final Field field;

    /**
     * Constructs a Group with the specified field.
     *
     * @param field The field to be associated with this group.
     */
    public Group(Field field) {
        this.field = field;
    }
}
