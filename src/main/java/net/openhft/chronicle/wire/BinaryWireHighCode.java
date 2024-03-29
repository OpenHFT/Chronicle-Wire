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
 * Enum to represent various high-level codes used in the binary wire protocol.
 * Each constant in this enum serves as a marker to categorize the range of values
 * used for decoding in the binary wire format. Although the enum itself does not
 * contain any instances (indicated by the empty enum body), it does define several
 * static final fields that serve as unique identifiers for each type of high-level code.
 */
public enum BinaryWireHighCode {
    ; // none

    /** Indicates the end of the data stream. */
    static final int END_OF_STREAM = -1;

    /** Represents numerical values, no longer in use. */
    static final int NUM0 = 0x0;

    /** Represents numerical values, no longer in use. */
    static final int NUM1 = 0x1;

    /** Represents numerical values, no longer in use. */
    static final int NUM2 = 0x2;

    /** Represents numerical values, no longer in use. */
    static final int NUM3 = 0x3;

    /** Represents numerical values, no longer in use. */
    static final int NUM4 = 0x4;

    /** Represents numerical values, no longer in use. */
    static final int NUM5 = 0x5;

    /** Represents numerical values, no longer in use. */
    static final int NUM6 = 0x6;

    /** Represents numerical values, no longer in use. */
    static final int NUM7 = 0x7;

    /** Indicates a control sequence. */
    static final int CONTROL = 0x8;

    /** Indicates a floating-point number. */
    static final int FLOAT = 0x9;

    /** Indicates an integer value. */
    static final int INT = 0xA;

    /** Indicates a special type. */
    static final int SPECIAL = 0xB;

    /** Represents fields of length 0 to 15. */
    static final int FIELD0 = 0xC;

    /** Represents fields of length 16 to 31. */
    static final int FIELD1 = 0xD;

    /** Represents strings of 0 to 15. */
    static final int STR0 = 0xE;

    /** Represents strings of 16 to 31. */
    static final int STR1 = 0xF;
}
