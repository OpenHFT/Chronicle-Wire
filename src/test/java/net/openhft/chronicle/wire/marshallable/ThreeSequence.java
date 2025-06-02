/*
 * Copyright 2016-2022 chronicle.software
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
    transient List<Rung> aBuffer = new ArrayList<>();
    @NotNull
    transient List<Rung> bBuffer = new ArrayList<>();
    @NotNull
    transient List<Rung> cBuffer = new ArrayList<>();

    // Lists that hold the actual serialized/deserialized data.
    @NotNull
    List<Rung> a = new ArrayList<>();
    @NotNull
    List<Rung> b = new ArrayList<>();
    @NotNull
    List<Rung> c = new ArrayList<>();

    // An optional text field associated with this object.
    @Nullable
    String text;

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
        text = wire.read("text").text();
    }
}
