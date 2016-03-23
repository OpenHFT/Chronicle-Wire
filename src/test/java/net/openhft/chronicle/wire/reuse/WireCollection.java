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

import net.openhft.chronicle.core.annotation.NotNull;
import net.openhft.chronicle.wire.*;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author gadei
 */
public class WireCollection extends WireModel implements Marshallable {

    private String reference;
    private String path;
    private String name;
    private Map<String, WireProperty> properties = new HashMap<>();
    private Map<String, WireCollection> collections = new HashMap<>();

    public WireCollection() {
    }

    public WireCollection(String reference, String path, String name, long id, int revision, String key) {
        super(id, revision, key);
        this.reference = reference;
        this.path = path;
        this.name = name;
    }

    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IllegalStateException {
        super.readMarshallable(wire);
        this.reference = wire.read(ModelKeys.reference).text();
        this.path = wire.read(ModelKeys.path).text();
        this.name = wire.read(ModelKeys.name).text();
        this.properties = wire.read(ModelKeys.properties).marshallableAsMap(WireProperty.class);
        this.collections = wire.read(ModelKeys.collections).marshallableAsMap(WireCollection.class);
    }

    @Override
    public void writeMarshallable(WireOut wire) {
        super.writeMarshallable(wire);
        wire
                .write(ModelKeys.reference).text(reference)
                .write(ModelKeys.path).text(path)
                .write(ModelKeys.name).text(name);
        if (properties.size() > 0) {
            wire.write(ModelKeys.properties).marshallableAsMap(properties, WireProperty.class);
        }
        if (collections.size() > 0) {
            wire.write(ModelKeys.collections).marshallableAsMap(collections, WireCollection.class, false);
        }
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

    public void clearProperties() {
        properties.clear();
    }

    public Map<String, WireProperty> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, WireProperty> properties) {
        this.properties = properties;
    }

    public void addProperty(WireProperty property) {
        this.properties.put(property.getReference(), property);
    }

    public Map<String, WireCollection> getCollections() {
        return collections;
    }

    public void setCollections(Map<String, WireCollection> collections) {
        this.collections = collections;
    }

    public void addCollection(WireCollection collection) {
        this.collections.put(collection.getReference(), collection);
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
