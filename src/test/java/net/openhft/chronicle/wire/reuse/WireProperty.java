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
 */
package net.openhft.chronicle.wire.reuse;

import net.openhft.chronicle.wire.WireIn;
import net.openhft.chronicle.wire.WireOut;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a property in a wire format, extending the WireModel.
 * Contains fields like reference, path, name, and value.
 */
public class WireProperty extends WireModel {

    // Nullable fields for the wire property
    @Nullable
    private String reference;
    @Nullable
    private String path;
    @Nullable
    private String name;
    @Nullable
    private String value;

    /**
     * Constructor for WireProperty.
     *
     * @param reference The reference of the wire property.
     * @param path The path of the wire property.
     * @param name The name of the wire property.
     * @param value The value of the wire property.
     * @param id Unique identifier.
     * @param revision Revision number.
     * @param key Key associated with the wire property.
     */
    public WireProperty(@Nullable String reference, @Nullable String path, @Nullable String name, @Nullable String value, long id, int revision, String key) {
        super(id, revision, key);
        this.reference = reference;
        this.path = path;
        this.name = name;
        this.value = value;
    }

    /**
     * Reads the property fields from a wire.
     *
     * @param wire The wire input to read from.
     * @throws IllegalStateException If reading from the wire fails.
     */
    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IllegalStateException {
        super.readMarshallable(wire);
        this.reference = wire.read(ModelKeys.reference).text();
        this.path = wire.read(ModelKeys.path).text();
        this.name = wire.read(ModelKeys.name).text();
        this.value = wire.read(ModelKeys.value).text();
    }

    /**
     * Writes the property fields to a wire.
     *
     * @param wire The wire output to write to.
     */
    @Override
    public void writeMarshallable(@NotNull WireOut wire) {
        super.writeMarshallable(wire);
        wire.write(ModelKeys.reference).text(reference)
                .write(ModelKeys.path).text(path)
                .write(ModelKeys.name).text(name)
                .write(ModelKeys.value).text(value);
    }

    // Getter and setter methods for reference, path, name, and value
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

    @Nullable
    public String getValue() {
        return value;
    }

    public void setValue(@Nullable String value) {
        this.value = value;
    }
}
