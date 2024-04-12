/*
 * Copyright 2016-2020 chronicle.software
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
package net.openhft.chronicle.wire;

/**
 * Represents the type of bracketing used, especially useful for indicating structures
 * like maps and sequences. This enumeration can be leveraged to denote the type or absence
 * of bracketing in specific contexts.
 */
public enum BracketType {

    /**
     * Represents an unknown bracketing type. Useful for cases where the type
     * is not yet determined or cannot be classified.
     */
    UNKNOWN,

    /**
     * Indicates that there is no bracketing used.
     */
    NONE,

    /**
     * Denotes the use of brackets typically associated with maps, e.g., '{}'.
     */
    MAP,

    /**
     * Denotes the use of brackets typically associated with sequences or lists, e.g., '[]'.
     */
    SEQ
}
