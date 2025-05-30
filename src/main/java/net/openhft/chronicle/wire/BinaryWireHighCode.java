/*
 * Copyright 2016-2025 chronicle.software
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
 * Defines constants representing the high four bits of a
 * {@link BinaryWireCode} byte. These high codes group the binary
 * wire codes into broad types such as Control, Float, Integer,
 * Special, Field, or String. This allows a parser to dispatch
 * efficiently, for example by switching on {@code code >> 4}.
 */
public enum BinaryWireHighCode {
    ; // none

    /**
     * Represents the end of the data stream. The value is {@code -1} and is not
     * a high code.
     */
    static final int END_OF_STREAM = -1;

    /**
     * Legacy high code for numerical values. Codes {@code 0x00-0x7F} normally
     * hold single-byte positive integers.
     */
    static final int NUM0 = 0x0;

    /** See {@link #NUM0}. */
    static final int NUM1 = 0x1;

    /** See {@link #NUM0}. */
    static final int NUM2 = 0x2;

    /** See {@link #NUM0}. */
    static final int NUM3 = 0x3;

    /** See {@link #NUM0}. */
    static final int NUM4 = 0x4;

    /** See {@link #NUM0}. */
    static final int NUM5 = 0x5;

    /** See {@link #NUM0}. */
    static final int NUM6 = 0x6;

    /** See {@link #NUM0}. */
    static final int NUM7 = 0x7;

    /**
     * High code {@code 0x80} for control sequences such as byte length
     * prefixes, padding and anchors.
     */
    static final int CONTROL = 0x8;

    /**
     * High code {@code 0x90} for floating-point numbers such as
     * {@link BinaryWireCode#FLOAT32} or {@link BinaryWireCode#FLOAT_STOP_6}.
     */
    static final int FLOAT = 0x9;

    /**
     * High code {@code 0xA0} for integer types such as
     * {@link BinaryWireCode#INT32}, {@link BinaryWireCode#UINT8} and
     * {@link BinaryWireCode#UUID}.
     */
    static final int INT = 0xA;

    /**
     * High code {@code 0xB0} for special values such as
     * {@link BinaryWireCode#NULL}, {@link BinaryWireCode#TRUE},
     * {@link BinaryWireCode#TYPE_PREFIX}, {@link BinaryWireCode#FIELD_NAME_ANY}
     * or {@link BinaryWireCode#EVENT_NAME}.
     */
    static final int SPECIAL = 0xB;

    /**
     * High code {@code 0xC0} for compact field name strings of length 0-15
     * (codes {@code 0xC0-0xCF}).
     */
    static final int FIELD0 = 0xC;

    /**
     * High code {@code 0xD0} for compact field name strings of length 16-31
     * (codes {@code 0xD0-0xDF}).
     */
    static final int FIELD1 = 0xD;

    /**
     * High code {@code 0xE0} for general strings of UTF-8 length 0-15
     * (codes {@code 0xE0-0xEF}).
     */
    static final int STR0 = 0xE;

    /**
     * High code {@code 0xF0} for general strings of UTF-8 length 16-31
     * (codes {@code 0xF0-0xFF}).
     */
    static final int STR1 = 0xF;
}
