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

@FunctionalInterface
public interface ReadMarshallable {
    /**
     * Straight line ordered decoding.
     *
     * @param wire to read from in an ordered manner.
     * @throws IllegalStateException the stream wasn't ordered or formatted as expected.
     */
    void readMarshallable(WireIn wire) throws IllegalStateException;

    ReadMarshallable DISCARD = w -> {
    };
}
