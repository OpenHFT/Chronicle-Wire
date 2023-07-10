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
 * The BinaryWireHighCode enumeration serves as a map of ranges for decoding a wire protocol.
 * Each constant corresponds to a different type or category of data expected in the protocol.
 * This map helps to streamline the process of data decoding.
 * <p>
 * The names of the constants give an indication of the type of data they represent:
 * NUM0-NUM7 correspond to numerical values,
 * CONTROL corresponds to control codes,
 * FLOAT and INT to floating point and integer values respectively,
 * SPECIAL corresponds to special characters or codes,
 * FIELD0 and FIELD1 to field data,
 * STR0 and STR1 to string data.
 * <p>
 * The END_OF_STREAM constant signifies the end of the data stream.
 */
public enum BinaryWireHighCode {
    ; // none
    /**
     * This constant represents the end of a data stream.
     */
    static final int END_OF_STREAM = -1;
    /**
     * This constant corresponds to the first numerical range in the data.
     * <p>
     * This usage is deprecated. Use {@link #INT} instead.
     */
    static final int NUM0 = 0x0;
    /**
     * This constant corresponds to the second numerical range in the data.
     * <p>
     * This usage is deprecated. Use {@link #INT} instead.
     */
    static final int NUM1 = 0x1;
    /**
     * This constant corresponds to the third numerical range in the data.
     * <p>
     * This usage is deprecated. Use {@link #INT} instead.
     */
    static final int NUM2 = 0x2;
    /**
     * This constant corresponds to the fourth numerical range in the data.
     * <p>
     * This usage is deprecated. Use {@link #INT} instead.
     */
    static final int NUM3 = 0x3;
    /**
     * This constant corresponds to the fifth numerical range in the data.
     * <p>
     * This usage is deprecated. Use {@link #INT} instead.
     */
    static final int NUM4 = 0x4;
    /**
     * This constant corresponds to the sixth numerical range in the data.
     * <p>
     * This usage is deprecated. Use {@link #INT} instead.
     */
    static final int NUM5 = 0x5;
    /**
     * This constant corresponds to the seventh numerical range in the data.
     * <p>
     * This usage is deprecated. Use {@link #INT} instead.
     */
    static final int NUM6 = 0x6;
    /**
     * This constant corresponds to the eighth numerical range in the data.
     * <p>
     * This usage is deprecated. Use {@link #INT} instead.
     */
    static final int NUM7 = 0x7;
    /**
     * This constant corresponds to control codes in the data.
     */
    static final int CONTROL = 0x8;
    /**
     * This constant corresponds to floating point numbers in the data.
     */
    static final int FLOAT = 0x9;
    /**
     * This constant corresponds to integer numbers in the data.
     */
    static final int INT = 0xA;
    /**
     * This constant corresponds to special codes in the data.
     */
    static final int SPECIAL = 0xB;
    /**
     * This constant corresponds to the first field data range in the data.
     */
    static final int FIELD0 = 0xC;
    /**
     * This constant corresponds to the second field data range in the data.
     */
    static final int FIELD1 = 0xD;
    /**
     * This constant corresponds to the first string data range in the data.
     */
    static final int STR0 = 0xE;
    /**
     * This constant corresponds to the second string data range in the data.
     */
    static final int STR1 = 0xF;
}
