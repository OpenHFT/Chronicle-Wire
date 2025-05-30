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

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;

/**
 * Defines the integer codes used by {@link BinaryWire} to denote data types and
 * control sequences. The first byte of each value is one of these constants
 * indicating how the following bytes are to be interpreted.
 */
public enum BinaryWireCode {
    ; // Indicates no default enum instances

    // Definitions for sequence lengths:
    /**
     * Indicates that an unsigned byte follows containing the length (0-255) of
     * a byte sequence.
     */
    public static final int BYTES_LENGTH8 = 0x80;

    /**
     * Indicates a two byte length field storing 0 to 2^16-1 bytes.
     */
    public static final int BYTES_LENGTH16 = 0x81;

    /**
     * Indicates a four byte length field storing up to 2^32-1 bytes.
     */
    public static final int BYTES_LENGTH32 = 0x82;

    /**
     * Indicates that a {@link VanillaMessageHistory} follows in its compact form.
     */
    public static final int HISTORY_MESSAGE = 0x86;

    /**
     * Indicates a field anchor point within the serialized structure.
     */
    @Deprecated
    public static final int FIELD_ANCHOR = 0x87;

    /**
     * Indicates a general anchor point within the serialized structure.
     */
    @Deprecated
    public static final int ANCHOR = 0x88;

    /**
     * Denotes an updated alias value.
     */
    @Deprecated
    public static final int UPDATED_ALIAS = 0x89;

    /**
     * Array of unsigned bytes.
     */
    public static final int U8_ARRAY = 0x8A;
    //        public static final int U16_ARRAY = 0x8B;
    //        public static final int I32_ARRAY = 0x8C;
    /**
     * Array of 64-bit integers.
     */
    public static final int I64_ARRAY = 0x8D;

    /**
     * padding with a 32-bit length to ensure alignment.
     */
    public static final int PADDING32 = 0x8E;

    /**
     * Single byte padding to ensure alignment.
     */
    public static final int PADDING = 0x8F;

    /**
     * A 32 bit IEEE-754 floating point value.
     */
    public static final int FLOAT32 = 0x90;

    /**
     * A 64 bit IEEE-754 floating point value.
     */
    public static final int FLOAT64 = 0x91;

    /**
     * Floating point encoded with 2 decimal places using stop bit compression.
     */
    public static final int FLOAT_STOP_2 = 0x92;

    /**
     * Floating point encoded with 4 decimal places using stop bit compression.
     */
    public static final int FLOAT_STOP_4 = 0x94;

    /**
     * Floating point encoded with 6 decimal places using stop bit compression.
     */
    public static final int FLOAT_STOP_6 = 0x96;

    /**
     * Floating point rounded to the nearest whole number for compact storage.
     */
    public static final int FLOAT_SET_LOW_0 = 0x9A;

    /**
     * Floating point rounded to two decimal places.
     */
    public static final int FLOAT_SET_LOW_2 = 0x9B;

    /**
     * Floating point rounded to four decimal places.
     */
    public static final int FLOAT_SET_LOW_4 = 0x9C;

    /**
     * Represents a universally unique identifier (UUID).
     */
    public static final int UUID = 0xA0;

    /**
     * Unsigned 8-bit integer (byte).
     */
    public static final int UINT8 = 0xA1;

    /**
     * Unsigned 16-bit integer (short).
     */
    public static final int UINT16 = 0xA2;

    /**
     * Unsigned 32-bit integer.
     */
    public static final int UINT32 = 0xA3;

    /**
     * Signed 8-bit integer (byte).
     */
    public static final int INT8 = 0xA4;

    /**
     * Signed 16-bit integer (short).
     */
    public static final int INT16 = 0xA5;

    /**
     * Signed 32-bit integer.
     */
    public static final int INT32 = 0xA6;

    /**
     * Signed 64-bit integer (long).
     */
    public static final int INT64 = 0xA7;

    /**
     * 8-bit integer with optimized storage for low positive values.
     */
    public static final int SET_LOW_INT8 = 0xA8;

    /**
     * 16-bit integer with optimized storage for low positive values.
     */
    public static final int SET_LOW_INT16 = 0xA9;
    //    public static final int FIXED_5 = 0xAA;
//    public static final int FIXED_4 = 0xAB;
//    public static final int FIXED_3 = 0xAC;
//    public static final int FIXED_2 = 0xAD;
//    public static final int FIXED_1 = 0xAE;
    /**
     * 64-bit integer to be displaying in hexadecimal format.
     */
    public static final int INT64_0x = 0xAF;

    /**
     * boolean value representing 'false'.
     */
    public static final int FALSE = 0xB0;

    /**
     * boolean value representing 'true'.
     */
    public static final int TRUE = 0xB1;

    /**
     * Represents a time value.
     */
    public static final int TIME = 0xB2;

    /**
     * Represents a date value.
     */
    public static final int DATE = 0xB3;

    /**
     * Represents a date-time value.
     */
    public static final int DATE_TIME = 0xB4;

    /**
     * Represents a zoned date-time value.
     */
    public static final int ZONED_DATE_TIME = 0xB5;

    /**
     * Prefix indicating the type of the following serialized object.
     */
    public static final int TYPE_PREFIX = 0xB6;

    /**
     * Indicates a field name of variable length encoded as an 8 bit string.
     */
    public static final int FIELD_NAME_ANY = 0xB7;

    /**
     * Indicates an arbitrary length UTF-8 string.
     */
    public static final int STRING_ANY = 0xB8;

    /**
     * Field name used to denote an event, length encoded as an 8 bit string.
     */
    public static final int EVENT_NAME = 0xB9;

    /**
     * Field identifier stored as an unsigned integer rather than text.
     */
    public static final int FIELD_NUMBER = 0xBA;
    /**
     * Represents the {@code null} value.
     */
    public static final int NULL = 0xBB;

    /**
     * Encodes a class literal as an 8 bit length followed by the UTF-8 text.
     */
    public static final int TYPE_LITERAL = 0xBC;

    /**
     * Signifies that an event payload follows rather than a simple string name.
     */
    public static final int EVENT_OBJECT = 0xBD;

    /**
     * Marks a comment that should be ignored by parsers.
     */
    public static final int COMMENT = 0xBE;

    /**
     * Hint used by the wire layer; consumers may ignore it.
     */
    public static final int HINT = 0xBF;

    /**
     * Start of the compact field name range. Codes {@code FIELD_NAME0} to
     * {@code FIELD_NAME31} represent names of length 0 to 31.
     */
    public static final int FIELD_NAME0 = 0xC0;

    /**
     * End of the compact field name range.
     */
    public static final int FIELD_NAME31 = 0xDF;

    /**
     * Start of the compact string range. Values {@code STRING_0} to {@code STRING_31}
     * encode text of length 0 to 31.
     */
    public static final int STRING_0 = 0xE0;

    /**
     * End of the compact string range.
     */
    public static final int STRING_31 = 0xFF;

    /**
     * Lookup table mapping a byte code to its mnemonic name. Used for debugging
     * and diagnostic output.
     */
    public static final String[] STRING_FOR_CODE = new String[256];

    // Static initializer to populate the STRING_FOR_CODE array:
    static {
        try {
            for (@NotNull Field field : BinaryWireCode.class.getDeclaredFields()) {
                if (field.getType() == int.class)
                    STRING_FOR_CODE[field.getInt(null)] = field.getName();
            }
            for (int i = FIELD_NAME0; i <= FIELD_NAME31; i++)
                STRING_FOR_CODE[i] = "FIELD_" + (i - FIELD_NAME0);
            for (int i = STRING_0; i <= STRING_31; i++)
                STRING_FOR_CODE[i] = "STRING_" + (i - STRING_0);
            for (int i = 0; i < STRING_FOR_CODE.length; i++) {
                if (STRING_FOR_CODE[i] == null)
                    if (i <= ' ' || i >= 127) {
                        STRING_FOR_CODE[i] = "Unknown_0x" + Integer.toHexString(i).toUpperCase();
                    } else {
                        STRING_FOR_CODE[i] = "Unknown_" + (char) i;
                    }
            }
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Returns {@code true} if the code denotes any form of field identifier
     * (numeric or textual).
     */
    public static boolean isFieldCode(int code) {
        return code == FIELD_NAME_ANY ||
                code == FIELD_NUMBER ||
                (code >= FIELD_NAME0 && code <= FIELD_NAME31);
    }

    /**
     * Returns a human readable name for the supplied code using
     * {@link #STRING_FOR_CODE}. A code of {@code -1} maps to "EndOfFile".
     */
    @NotNull
    public static String stringForCode(int code) {
        return code == -1 ? "EndOfFile" : STRING_FOR_CODE[code];
    }
}
