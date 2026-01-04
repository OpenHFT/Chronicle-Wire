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
 * Class representing a nested structure for testing partial serialization and deserialization in Chronicle Wire.
 * It demonstrates custom read logic for marshalling a subset of fields and writing additional fields.
 */
public class NestedReadSubset implements Marshallable {
    private String text;
    private String text2 = "";
    private double number;
    private double number2;
    private double number4;

    /**
     * Custom read logic for marshalling from Wire. It specifies how a subset of fields
     * (text and number) of the class is read from the wire.
     *
     * @param wire The WireIn instance to read the data from.
     * @throws IORuntimeException If an IO error occurs during reading.
     */
    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
        String text = wire.read("text").text();
        double number = wire.read("number").float64();
        setTextNumber(text, number);
    }

    /**
     * Custom write logic for marshalling to Wire. It specifies how fields of the class
     * are written to the wire, including additional fields not present in the read logic.
     *
     * @param wire The WireOut instance to write the data to.
     */
    @Override
    public void writeMarshallable(@NotNull WireOut wire) {
        // Writing more fields than are read
        wire.write(() -> "text").text(text)
                .write(() -> "number").float64(number)
                .write(() -> "text3").text("is text3")
                .write(() -> "number4").float64(number4)
                .write(() -> "number3").float64(333.3);
    }

    /**
     * Sets the text and number fields and calculates number4.
     *
     * @param text   The text to set.
     * @param number The number to set.
     * @return This NestedReadSubset instance for method chaining.
     */
    public NestedReadSubset setTextNumber(String text, double number) {
        this.text = text;
        this.number = number;
        this.number4 = number * 4;
        return this;
    }

    /**
     * Provides a string representation of this class, including all its fields.
     *
     * @return String representation of this class.
     */
    @NotNull
    @Override
    public String toString() {
        return "NestedReadSubset{" +
                "text='" + text + '\'' +
                ", text2='" + text2 + '\'' +
                ", number=" + number +
                ", number2=" + number2 +
                ", number4=" + number4 +
                '}';
    }
}
