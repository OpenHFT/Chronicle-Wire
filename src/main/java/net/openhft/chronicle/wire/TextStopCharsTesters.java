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

import net.openhft.chronicle.bytes.StopCharsTester;

/**
 * Created by peter on 16/08/15.
 */
enum TextStopCharsTesters implements StopCharsTester {
    END_OF_TEXT {
        @Override
        public boolean isStopChar(int ch, int ch2) throws IllegalStateException {
            // one character stop.
            if (ch == '"' || ch == '#' || ch == '\n' || ch == '}') return true;
            // two character stop.
            return (ch == ':' || ch == ',') && (ch2 <= ' ' || ch2 == '"');
        }
    },
}
