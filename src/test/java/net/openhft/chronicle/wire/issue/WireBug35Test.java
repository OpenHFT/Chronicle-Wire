/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * @author ryanlea
 */
@SuppressWarnings({"deprecation", "removal"})
class WireBug35Test extends WireTestCommon {

    @Test
    @DisplayName("Text wire should write objects in sequence")
    void objectsInSequence() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory is required for sequence tests");

        assertObjectsInSequence(WireType.TEXT.apply(Bytes.elasticByteBuffer()));
    }

    @Test
    @DisplayName("Binary wire should write objects in sequence")
    void objectsInSequenceBinaryWire() {
        final Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        assertObjectsInSequence(WireType.BINARY.apply(bytes));
    }

    private void assertObjectsInSequence(Wire wire) {
        wire.write(() -> "seq").sequence(seq -> {
            seq.marshallable(obj -> obj.write(() -> "key").text("value"));
            seq.marshallable(obj -> obj.write(() -> "key").text("value"));
        });

        final String text = Wires.asText(wire, Bytes.allocateElasticOnHeap()).toString();
        Object load = new Yaml().load(text);

        assertEquals("{seq=[{key=value}, {key=value}]}", load.toString(),
                "Sequence should render expected YAML text");

        wire.bytes().releaseLast();
    }
}
