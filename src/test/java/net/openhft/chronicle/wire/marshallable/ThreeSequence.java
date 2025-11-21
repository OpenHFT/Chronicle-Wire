/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.WireIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a structured data object with three sequences/lists of {@link Rung} objects
 * and an optional text field. It extends SelfDescribingMarshallable for serialization and
 * deserialization using the Chronicle Wire library.
 */
class ThreeSequence extends SelfDescribingMarshallable {

    // Transient buffers to temporarily hold data during the marshalling process.
    // These buffers are not serialized because of the 'transient' modifier.
    @NotNull
    private final transient List<Rung> aBuffer = new ArrayList<>();
    @NotNull
    private final transient List<Rung> bBuffer = new ArrayList<>();
    @NotNull
    private final transient List<Rung> cBuffer = new ArrayList<>();

    // Lists that hold the actual serialized/deserialized data.
    @NotNull
    private final
    List<Rung> a = new ArrayList<>();
    @NotNull
    private final
    List<Rung> b = new ArrayList<>();
    @NotNull
    private final
    List<Rung> c = new ArrayList<>();

    /**
     * Custom method to deserialize data from the provided wire.
     * The sequence(...) method is used to read lists of objects from the wire.
     *
     * @param wire The input wire source to read from.
     * @throws IORuntimeException if any IO errors occur during deserialization.
     */
    @Override
    public void readMarshallable(@NotNull WireIn wire) throws IORuntimeException {
        // Reads the sequence "b" from the wire into the list 'b' using 'bBuffer' as a buffer.
        wire.read("b").sequence(b, bBuffer, Rung::new);
        // Reads the sequence "a" from the wire into the list 'a' using 'aBuffer' as a buffer.
        wire.read("a").sequence(a, aBuffer, Rung::new);
        // Reads the sequence "c" from the wire into the list 'c' using 'cBuffer' as a buffer.
        wire.read("c").sequence(c, cBuffer, Rung::new);
        // Reads the "text" field from the wire.
        // An optional text field associated with this object.
        @Nullable String text = wire.read("text").text();
    }
}
