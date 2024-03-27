/*
 * Copyright 2016-2020 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.openhft.chronicle.wire.reuse;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.WireIn;
import net.openhft.chronicle.wire.WireOut;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for models in the Wire framework, providing common functionality
 * for serialization and deserialization, along with basic model properties.
 */
public class WireModel extends SelfDescribingMarshallable {
    // Unique identifier for the model
    private long id;
    // Revision number for versioning of the model
    private int revision;
    // Optional key for additional identification
    @Nullable
    private String key;

    /**
     * Default constructor for creating an instance of WireModel.
     */
    public WireModel() {
    }

    /**
     * Constructor with parameters to initialize the model's properties.
     *
     * @param id       The unique identifier for the model.
     * @param revision The revision number for the model.
     * @param key      Optional key for additional identification.
     */
    public WireModel(long id, int revision, @Nullable String key) {
        this.id = id;
        this.revision = revision;
        this.key = key;
    }

    /**
     * Reads the properties of this model from a Wire input source.
     *
     * @param wire The Wire input source to read from.
     * @throws IllegalStateException if reading from the Wire source fails.
     */
    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IllegalStateException {
        this.id = wire.read(ModelKeys.id).int64();
        this.revision = wire.read(ModelKeys.revision).int32();
        this.key = wire.read(ModelKeys.key).text();
    }

    /**
     * Writes the properties of this model to a Wire output destination.
     *
     * @param wire The Wire output destination to write to.
     */
    @Override
    public void writeMarshallable(@NotNull WireOut wire) {
        wire.write(ModelKeys.id).int64(id)
                .write(ModelKeys.revision).int32(revision)
                .write(ModelKeys.key).text(key);
    }

    // Getters and setters for the model's properties.

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

    @Nullable
    public String getKey() {
        return key;
    }

    public void setKey(@Nullable String key) {
        this.key = key;
    }
}
