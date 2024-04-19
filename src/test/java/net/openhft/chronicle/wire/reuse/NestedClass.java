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

import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.WireIn;
import net.openhft.chronicle.wire.WireOut;
import org.jetbrains.annotations.NotNull;

/**
 * NestedClass is a simple data class that implements the Marshallable interface to
 * enable serialization and deserialization using Chronicle Wire.
 * It contains two fields: a String 'text' and a double 'number'.
 */
public class NestedClass implements Marshallable {
    // Field to store text data
    String text;
    // Field to store numeric data
    double number;

    /**
     * Reads data from a WireIn instance and populates the fields of this class.
     * This method overrides from Marshallable interface for custom deserialization logic.
     *
     * @param wire The WireIn instance to read data from.
     * @throws IORuntimeException If there is an error during read operation.
     */
    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
        wire.read(() -> "text").text(this, (t, v) -> t.text = v)
                .read(() -> "number").float64(this, (t, v) -> t.number = v);
    }

    /**
     * Writes the current state of this object's fields to a WireOut instance.
     * This method overrides from Marshallable interface for custom serialization logic.
     *
     * @param wire The WireOut instance to write data to.
     */
    @Override
    public void writeMarshallable(@NotNull WireOut wire) {
        wire.write(() -> "text").text(text)
                .write(() -> "number").float64(number);
    }

    /**
     * Sets the text and number fields of this class.
     *
     * @param text The text to set.
     * @param number The numeric value to set.
     */
    public void setTextNumber(String text, double number) {
        this.text = text;
        this.number = number;
    }

    /**
     * Provides a string representation of this object including its field values.
     *
     * @return A string representation of this NestedClass instance.
     */
    @NotNull
    @Override
    public String toString() {
        return "NestedClass{" +
                "text='" + text + '\'' +
                ", number=" + number +
                '}';
    }
}
