/*
 * Copyright 2016-2020 Chronicle Software
 *
 * https://chronicle.software
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
 * These are the ranges of values to help decode the protocol.
 */
public enum BinaryWireHighCode {
    ;
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
