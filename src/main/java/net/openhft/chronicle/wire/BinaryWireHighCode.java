//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//
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

    /**
     * High code for control sequences such as byte length
     * prefixes, padding and anchors.
     */
    static final int CONTROL = 0x8;

    /**
     * High code for floating-point numbers such as
     * {@link BinaryWireCode#FLOAT32} or {@link BinaryWireCode#FLOAT_STOP_6}.
     */
    static final int FLOAT = 0x9;

    /**
     * High code for integer types such as
     * {@link BinaryWireCode#INT32}, {@link BinaryWireCode#UINT8} and
     * {@link BinaryWireCode#UUID}.
     */
    static final int INT = 0xA;

    /**
     * High code for special values such as
     * {@link BinaryWireCode#NULL}, {@link BinaryWireCode#TRUE} or
     * {@link BinaryWireCode#TYPE_PREFIX}.
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
