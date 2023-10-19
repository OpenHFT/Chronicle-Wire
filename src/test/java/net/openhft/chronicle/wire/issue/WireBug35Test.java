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

package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.scoped.ScopedResource;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import net.openhft.chronicle.wire.Wires;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;

/**
 * @author ryanlea
 */
public class WireBug35Test extends WireTestCommon {

    @Test
    public void objectsInSequence() {
        final Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();

        final Wire wire = WireType.TEXT.apply(bytes);
        wire.write(() -> "seq").sequence(seq -> {
            seq.marshallable(obj -> obj.write(() -> "key").text("value"));
            seq.marshallable(obj -> obj.write(() -> "key").text("value"));
        });

        try (final ScopedResource<Bytes<?>> bytesTl = Wires.acquireBytesScoped()) {
            @NotNull final String text = Wires.asText(wire, bytesTl.get()).toString();
            Object load = new Yaml().load(text);

            assertEquals("{seq=[{key=value}, {key=value}]}", load.toString());

            bytes.releaseLast();
        }
    }

    @Test
    public void objectsInSequenceBinaryWire() {
        final Bytes<ByteBuffer> bytes = Bytes.elasticByteBuffer();
        final Wire wire = WireType.BINARY.apply(bytes);
        wire.write(() -> "seq").sequence(seq -> {
            seq.marshallable(obj -> obj.write(() -> "key").text("value"));
            seq.marshallable(obj -> obj.write(() -> "key").text("value"));
        });

        try (final ScopedResource<Bytes<?>> bytesTl = Wires.acquireBytesScoped()) {
            @NotNull final String text = Wires.asText(wire, bytesTl.get()).toString();
            Object load = new Yaml().load(text);

            assertEquals("{seq=[{key=value}, {key=value}]}", load.toString());
        }

        bytes.releaseLast();
    }
}