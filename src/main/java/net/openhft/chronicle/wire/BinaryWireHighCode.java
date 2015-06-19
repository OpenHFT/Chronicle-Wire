/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.chronicle.wire;

/**
 * Created by peter on 18/05/15.
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
