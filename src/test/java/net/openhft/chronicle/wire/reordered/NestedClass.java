/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.reordered;

import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.Marshallable;
import net.openhft.chronicle.wire.WireIn;
import net.openhft.chronicle.wire.WireOut;
import org.jetbrains.annotations.NotNull;

/**
 * Class representing a nested structure for testing serialization and deserialization in Chronicle Wire.
 * It demonstrates custom read and write logic for marshalling, handling nested objects and different data types.
 */
@SuppressWarnings({"deprecation", "removal"})
public class NestedClass implements Marshallable {
    private String text;
    private String text2;
    private double number;
    private double number2;
    private double number4; // out of order
    private NestedClass doublyNested;

    /**
     * Custom read logic for marshalling from Wire. It specifies how each field of the class
     * is read from the wire in a defined order and format.
     *
     * @param wire The WireIn instance to read the data from.
     * @throws IORuntimeException If an IO error occurs during reading.
     */
    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
        wire.read(() -> "text").text(this, (t, v) -> t.text = v)
                .read(() -> "text2").text(this, (t, v) -> t.text2 = v)
                .read(() -> "doublyNested").object(NestedClass.class, this, (t, v) -> t.doublyNested = v)
                .read(() -> "number").float64(this, (t, v) -> t.number = v)
                .read(() -> "number2").float64(this, (t, v) -> t.number2 = v)
                .read(() -> "number4").float64(this, (t, v) -> t.number4 = v);
    }

    /**
     * Custom write logic for marshalling to Wire. It specifies how each field of the class
     * is written to the wire in a defined order and format. Note that it includes fields not present in the read logic.
     *
     * @param wire The WireOut instance to write the data to.
     */
    @Override
    public void writeMarshallable(@NotNull WireOut wire) {
        // write version has 2 extra fields but is missing two fields
        wire.write(() -> "text").text(text)
                .write(() -> "text3").text("is text3")
                .write(() -> "number4").float64(number4)
                .write(() -> "number").float64(number)
                .write(() -> "doublyNested").object(doublyNested)
                .write(() -> "number3").float64(333.3);
    }

    /**
     * Sets the text and number fields and calculates number4.
     *
     * @param text   The text to set.
     * @param number The number to set.
     * @return This NestedClass instance for method chaining.
     */
    public NestedClass setTextNumber(String text, double number) {
        this.text = text;
        this.number = number;
        this.number4 = number * 4;
        return this;
    }

    /**
     * Creates and nests a new NestedClass instance with the given text and number.
     *
     * @param text   The text for the nested instance.
     * @param number The number for the nested instance.
     */
    public void nest(String text, double number) {
        this.doublyNested = new NestedClass().setTextNumber(text, number);
    }

    /**
     * Provides a string representation of this class, including all its fields.
     *
     * @return String representation of this class.
     */
    @NotNull
    @Override
    public String toString() {
        return "NestedClass{" +
                "text='" + text + '\'' +
                ", text2='" + text2 + '\'' +
                ", number=" + number +
                ", number2=" + number2 +
                ", number4=" + number4 +
                '}';
    }
}
