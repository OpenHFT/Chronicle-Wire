/*
 *     Copyright (C) 2015-2020 chronicle.software
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

/* Generated SBE (Simple Binary Encoding) message codec */
package baseline;

public enum Side {
    Buy((short) 0),
    Sell((short) 1),
    NULL_VAL((short) 255);

    private final short value;

    Side(final short value) {
        this.value = value;
    }

    public static Side get(final short value) {
        switch (value) {
            case 0:
                return Buy;
            case 1:
                return Sell;
        }

        if ((short) 255 == value) {
            return NULL_VAL;
        }

        throw new IllegalArgumentException("Unknown value: " + value);
    }

    public short value() {
        return value;
    }
}
