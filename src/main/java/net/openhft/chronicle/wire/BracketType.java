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
 * Represents the type of bracketing used in serialized data formats, particularly
 * in the context of Chronicle Wire serialization/deserialization processes. This enumeration
 * helps in identifying the structural format (e.g., maps, sequences) of the data or the absence
 * thereof, facilitating appropriate parsing and writing operations based on the data type.
 */
public enum BracketType {

    /**
     * Indicates an unknown bracketing type. This is typically used in scenarios where the
     * bracketing type cannot be determined due to the data not adhering to expected formats,
     * or when the parsing process has not yet identified the data structure. It serves as a
     * placeholder for error handling and initial states before the actual bracketing type is
     * recognized.
     */
    UNKNOWN,

    /**
     * Signifies that no bracketing is used. This bracket type is applicable in situations where
     * the data does not conform to structured types like maps or sequences. For instance, it might
     * be used for simple values or in cases where the data structure is flat and does not require
     * nested bracketing. It's also the default type for certain data entries that do not fit
     * traditional bracketed structures.
     */
    NONE,

    /**
     * Denotes the use of curly brackets '{}' associated with maps. This bracket type is utilized
     * when parsing or writing key-value pairs, indicating that the data should be interpreted as
     * a map structure. It is critical for handling complex, structured data that benefits from
     * key-based access, enabling efficient serialization and deserialization of map-like data
     * constructs.
     */
    MAP,

    /**
     * Denotes the use of square brackets '[]' typically associated with sequences or lists.
     * This bracket type indicates that the data should be treated as an ordered collection,
     * suitable for iterating or accessing elements by index. It is essential for representing
     * lists of items, facilitating the serialization and deserialization of sequences in a
     * manner that preserves the order and structure of the collection.
     */
    SEQ,

    /**
     * Represents a special case used internally for processing history messages. This type is
     * leveraged in specific contexts where the data pertains to historical records or messages
     * that require unique handling. It indicates a custom structure or processing rule that
     * differs from conventional maps or sequences, tailored to the requirements of history
     * message serialization and deserialization.
     */
    HISTORY_MESSAGE
}
