/*
 *
 *  *     Copyright (C) ${YEAR}  higherfrequencytrading.com
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
import net.openhft.chronicle.wire.WireType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by peter.lawrey on 01/02/2016.
 */
public class OuterClass implements Marshallable {
    final List<NestedClass> listAFree = new ArrayList<>();
    final List<NestedClass> listA = new ArrayList<>();
    final List<NestedClass> listBFree = new ArrayList<>();
    final List<NestedClass> listB = new ArrayList<>();
    String text;
    WireType wireType;

    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
        wire.read(() -> "text").text(this, (t, v) -> t.text = v)
                .read(() -> "wireType").object(WireType.class, this, (t, v) -> t.wireType = v)
                .read(() -> "listA").sequence(this, (t, v) -> {
            t.clearListA();
            while (v.hasNextSequenceItem())
                v.marshallable(addListA());
            Thread.yield();
        })
                .read(() -> "listB").sequence(this, (t, v) -> {
            t.clearListB();
            while (v.hasNextSequenceItem())
                v.marshallable(addListB());
        });
    }

    @Override
    public void writeMarshallable(@NotNull WireOut wire) {
        wire.write(() -> "text").text(text)
                .write(() -> "wireType").text(wireType.name())
                .write(() -> "listA").sequence(this, (t, v) -> {
            for (NestedClass nc : t.getListA()) {
                v.marshallable(nc);
            }
        })
                .write(() -> "listB").sequence(this, (t, v) -> {
            for (NestedClass nc : t.getListB()) {
                v.marshallable(nc);
            }
        });

    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public WireType getWireType() {
        return wireType;
    }

    public void setWireType(WireType wireType) {
        this.wireType = wireType;
    }

    public List<NestedClass> getListA() {
        return listA;
    }

    public void clearListA() {
        listA.clear();
    }

    public NestedClass addListA() {
        if (listAFree.size() <= listA.size())
            listAFree.add(new NestedClass());
        NestedClass nc = listAFree.get(listA.size());
        listA.add(nc);
        return nc;
    }

    public List<NestedClass> getListB() {
        return listB;
    }

    public void clearListB() {
        listB.clear();
    }

    public NestedClass addListB() {
        if (listBFree.size() <= listB.size())
            listBFree.add(new NestedClass());
        NestedClass nc = listBFree.get(listB.size());
        listB.add(nc);
        return nc;
    }

    @Override
    public String toString() {
        return "OuterClass{" +
                "text='" + text + '\'' +
                ", wireType=" + wireType +
                ", listA=" + listA +
                ", listB=" + listB +
                '}';
    }
}
