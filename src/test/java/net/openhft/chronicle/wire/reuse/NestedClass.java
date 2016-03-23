/*
 *
 *  *     Copyright (C) 2016  higherfrequencytrading.com
 *  *
 *  *     This program is free software: you can redistribute it and/or modify
 *  *     it under the terms of the GNU Lesser General Public License as published by
 *  *     the Free Software Foundation, either version 3 of the License.
 *  *
 *  *     This program is distributed in the hope that it will be useful,
 *  *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  *     GNU Lesser General Public License for more details.
 *  *
 *  *     You should have received a copy of the GNU Lesser General Public License
 *  *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package net.openhft.chronicle.wire.reuse;

import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.WireIn;
import net.openhft.chronicle.wire.WireOut;
import org.jetbrains.annotations.NotNull;

/**
 * Created by peter.lawrey on 01/02/2016.
 */
public class NestedClass implements Marshallable {
    String text;
    double number;

    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
        wire.read(() -> "text").text(this, (t, v) -> t.text = v)
                .read(() -> "number").float64(this, (t, v) -> t.number = v);
    }

    @Override
    public void writeMarshallable(@NotNull WireOut wire) {
        wire.write(() -> "text").text(text)
                .write(() -> "number").float64(number);
    }

    public void setTextNumber(String text, double number) {
        this.text = text;
        this.number = number;
    }

    @Override
    public String toString() {
        return "NestedClass{" +
                "text='" + text + '\'' +
                ", number=" + number +
                '}';
    }
}
