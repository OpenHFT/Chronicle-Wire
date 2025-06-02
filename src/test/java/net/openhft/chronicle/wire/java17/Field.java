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

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a field with the ability to describe its structure and contents.
 * Inherits the capabilities of SelfDescribingMarshallable to provide
 * self-describing marshalling and unmarshalling.
 */
public class Field extends SelfDescribingMarshallable {

    // A map to maintain the required status for various field names
    private final Map<String, Required> required = new HashMap<>();

    /**
     * Sets the required status for a given field name.
     *
     * @param name     The name of the field.
     * @param required The required status of the field.
     */
    public void required(String name, Required required) {
        this.required.put(name, required);
    }
}
