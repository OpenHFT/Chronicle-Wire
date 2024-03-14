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

package net.openhft.chronicle.wire.method;

/**
 * Enum representing different service types.
 * Each enum constant represents a distinct service.
 */
public enum Service {

    // Enum constants representing different services
    S1, // Represents service type S1
    S2; // Represents service type S2

    /**
     * Returns the service ID as a string.
     * This method converts the enum constant to a string representation.
     *
     * @return The string representation of the enum constant, effectively the service ID.
     */
    String serviceId() {
        return toString(); // Returns the string representation of the enum constant
    }
}
