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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.openhft.chronicle.wire.reuse;

import net.openhft.chronicle.core.annotation.NotNull;
import net.openhft.chronicle.wire.*;

/**
 * @author gadei
 */
public class WireModel implements Marshallable {
    private long id;
    private int revision;
    private String key;

    public WireModel() {
    }

    public WireModel(long id, int revision, String key) {
        this.id = id;
        this.revision = revision;
        this.key = key;
    }

    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IllegalStateException {
        this.id = wire.read(ModelKeys.id).int64();
        this.revision = wire.read(ModelKeys.revision).int32();
        this.key = wire.read(ModelKeys.key).text();
    }

    @Override
    public void writeMarshallable(WireOut wire) {
        wire.write(ModelKeys.id).int64(id)
                .write(ModelKeys.revision).int32(revision)
                .write(ModelKeys.key).text(key);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getRevision() {
        return revision;
    }

    public void setRevision(int revision) {
        this.revision = revision;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
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
