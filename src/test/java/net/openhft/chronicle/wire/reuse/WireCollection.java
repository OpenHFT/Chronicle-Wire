/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire.reuse;

import net.openhft.chronicle.core.annotation.NotNull;
import net.openhft.chronicle.wire.WireIn;
import net.openhft.chronicle.wire.WireOut;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author gadei
 */
public class WireCollection extends WireModel {

    @Nullable
    private String reference;
    @Nullable
    private String path;
    @Nullable
    private String name;
    @Nullable
    private Map<String, WireProperty> properties = new HashMap<>();
    @Nullable
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
        this.properties = wire.read(ModelKeys.properties).marshallableAsMap(String.class, WireProperty.class);
        this.collections = wire.read(ModelKeys.collections).marshallableAsMap(String.class, WireCollection.class);
    }

    @Override
    public void writeMarshallable(WireOut wire) {
        super.writeMarshallable(wire);
        wire
                .write(ModelKeys.reference).text(reference)
                .write(ModelKeys.path).text(path)
                .write(ModelKeys.name).text(name);
        if (properties.size() > 0) {
            wire.write(ModelKeys.properties).marshallable(properties, String.class, WireProperty.class, true);
        }
        if (collections != null && collections.size() > 0) {
            wire.write(ModelKeys.collections).marshallable(collections, String.class, WireCollection.class, false);
        }
    }

    @Nullable
    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    @Nullable
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void clearProperties() {
        properties.clear();
    }

    @Nullable
    public Map<String, WireProperty> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, WireProperty> properties) {
        this.properties = properties;
    }

    public void addProperty(@org.jetbrains.annotations.NotNull WireProperty property) {
        this.properties.put(property.getReference(), property);
    }

    @Nullable
    public Map<String, WireCollection> getCollections() {
        return collections;
    }

    public void setCollections(Map<String, WireCollection> collections) {
        this.collections = collections;
    }

    public void addCollection(@org.jetbrains.annotations.NotNull WireCollection collection) {
        this.collections.put(collection.getReference(), collection);
    }
}
