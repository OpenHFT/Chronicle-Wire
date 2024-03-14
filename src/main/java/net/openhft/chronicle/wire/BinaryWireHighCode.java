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
 *
 * <p><strong>High-Level Code Definitions:</strong></p>
 * <ul>
 *     <li>{@code END_OF_STREAM} (-1): Indicates the end of the data stream.</li>
 *     <li>{@code NUM0} to {@code NUM7} (0x0 to 0x7): Represent numerical values.</li>
 *     <li>{@code CONTROL} (0x8): Indicates a control sequence.</li>
 *     <li>{@code FLOAT} (0x9): Indicates a floating-point number.</li>
 *     <li>{@code INT} (0xA): Indicates an integer value.</li>
 *     <li>{@code SPECIAL} (0xB): Indicates a special type.</li>
 *     <li>{@code FIELD0} and {@code FIELD1} (0xC, 0xD): Represent fields.</li>
 *     <li>{@code STR0} and {@code STR1} (0xE, 0xF): Represent string types.</li>
 * </ul>
 */
public enum BinaryWireHighCode {
    ; // none
    static final int END_OF_STREAM = -1;
    static final int NUM0 = 0x0;
    static final int NUM1 = 0x1;
    static final int NUM2 = 0x2;
    static final int NUM3 = 0x3;
    static final int NUM4 = 0x4;
    static final int NUM5 = 0x5;
    static final int NUM6 = 0x6;
    static final int NUM7 = 0x7;
    static final int CONTROL = 0x8;
    static final int FLOAT = 0x9;
    static final int INT = 0xA;
    static final int SPECIAL = 0xB;
    static final int FIELD0 = 0xC;
    static final int FIELD1 = 0xD;
    static final int STR0 = 0xE;
    static final int STR1 = 0xF;
}
