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

import net.openhft.chronicle.core.annotation.NotNull;
import net.openhft.chronicle.wire.*;

/**
 * @author Gadi Eichhorn
 */
public class WireProperty extends WireModel implements Marshallable {

    private String reference;
    private String path;
    private String name;
    private String value;

    public WireProperty() {
    }

    public WireProperty(String reference, String path, String name, String value, long id, int revision, String key) {
        super(id, revision, key);
        this.reference = reference;
        this.path = path;
        this.name = name;
        this.value = value;
    }

    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IllegalStateException {
        super.readMarshallable(wire);
        this.reference = wire.read(ModelKeys.reference).text();
        this.path = wire.read(ModelKeys.path).text();
        this.name = wire.read(ModelKeys.name).text();
        this.value = wire.read(ModelKeys.value).text();
    }

    @Override
    public void writeMarshallable(WireOut wire) {
        super.writeMarshallable(wire);
        wire.write(ModelKeys.reference).text(reference)
                .write(ModelKeys.path).text(path)
                .write(ModelKeys.name).text(name)
                .write(ModelKeys.value).text(value);
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public int hashCode() {
        return HashWire.hash32(this);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof WriteMarshallable &&
                obj.toString().equals(toString());
    }

    @Override
    public String toString() {
        return WireType.TEXT.asString(this);
    }
}
