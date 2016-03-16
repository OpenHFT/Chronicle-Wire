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

import java.io.Serializable;

/**
 * Created by peter on 27/08/15.
 */
public class EndOfDayShort extends AbstractMarshallable implements Serializable {
    // Symbol,Company,Price,Change,ChangePercent,Day's Volume
    public String name;
    public double closingPrice, change, changePercent;
    long daysVolume;

    // TODO FIX the formatting of leaf nodes so this looks ok.
    @Override
    public void writeMarshallable(WireOut wire) {
        wire.write(() -> "name").text(name)
                .write(() -> "price").float64(closingPrice)
                .write(() -> "change").float64(change)
                .write(() -> "changePercent").float64(changePercent)
                .write(() -> "daysVolume").int64(daysVolume);
    }
}
