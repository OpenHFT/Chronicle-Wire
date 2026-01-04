/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.reuse;

import net.openhft.chronicle.wire.WireIn;
import net.openhft.chronicle.wire.WireOut;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a collection in the Wire model with various properties, including a reference, path, and name.
 * It can also contain properties and sub-collections.
 */
public class WireCollection extends WireModel {

    // Reference to another element or object in the model.
    @Nullable
    private String reference;

    // Path identifier for this collection.
    @Nullable
    private String path;

    // Name of this collection.
    @Nullable
    private String name;

    // A map of properties associated with this collection.
    @Nullable
    private Map<String, WireProperty> properties = new HashMap<>();

    // A map of sub-collections nested within this collection.
    @Nullable
    private Map<String, WireCollection> collections = new HashMap<>();

    /**
     * Default constructor for creating an instance of WireCollection.
     */
    public WireCollection() {
    }

    /**
     * Constructs a WireCollection with specified details.
     *
     * @param reference Reference to another element or object.
     * @param path      Path identifier for the collection.
     * @param name      Name of the collection.
     * @param id        Unique identifier for this collection.
     * @param revision  Revision number of this collection.
     * @param key       Key associated with this collection.
     */
    public WireCollection(@Nullable String reference, @Nullable String path, @Nullable String name, long id, int revision, String key) {
        super(id, revision, key);
        this.reference = reference;
        this.path = path;
        this.name = name;
    }

    /**
     * Reads the state of this WireCollection from a Wire input.
     *
     * @param wire The Wire input to read from.
     * @throws IllegalStateException if an error occurs during reading.
     */
    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IllegalStateException {
        super.readMarshallable(wire);
        this.reference = wire.read(ModelKeys.reference).text();
        this.path = wire.read(ModelKeys.path).text();
        this.name = wire.read(ModelKeys.name).text();
        this.properties = wire.read(ModelKeys.properties).marshallableAsMap(String.class, WireProperty.class);
        this.collections = wire.read(ModelKeys.collections).marshallableAsMap(String.class, WireCollection.class);
    }

    /**
     * Writes the state of this WireCollection to a Wire output.
     *
     * @param wire The Wire output to write to.
     */
    @Override
    public void writeMarshallable(@NotNull WireOut wire) {
        super.writeMarshallable(wire);
        wire
                .write(ModelKeys.reference).text(reference)
                .write(ModelKeys.path).text(path)
                .write(ModelKeys.name).text(name);
        if (!properties.isEmpty()) {
            wire.write(ModelKeys.properties).marshallable(properties, String.class, WireProperty.class, true);
        }
        if (collections != null && !collections.isEmpty()) {
            wire.write(ModelKeys.collections).marshallable(collections, String.class, WireCollection.class, false);
        }
    }

    @Nullable
    public String getReference() {
        return reference;
    }

    public void setReference(@Nullable String reference) {
        this.reference = reference;
    }

    @Nullable
    public String getPath() {
        return path;
    }

    public void setPath(@Nullable String path) {
        this.path = path;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    public void clearProperties() {
        properties.clear();
    }

    @Nullable
    public Map<String, WireProperty> getProperties() {
        return properties;
    }

    public void setProperties(@Nullable Map<String, WireProperty> properties) {
        this.properties = properties;
    }

    public void addProperty(@NotNull WireProperty property) {
        this.properties.put(property.getReference(), property);
    }

    @Nullable
    public Map<String, WireCollection> getCollections() {
        return collections;
    }

    public void setCollections(@Nullable Map<String, WireCollection> collections) {
        this.collections = collections;
    }

    public void addCollection(@NotNull WireCollection collection) {
        this.collections.put(collection.getReference(), collection);
    }
}
