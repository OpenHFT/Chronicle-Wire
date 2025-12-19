/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.util.IgnoresEverything;
import net.openhft.chronicle.wire.TextWire;
import net.openhft.chronicle.wire.Wire;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Test class extending WireTestCommon to verify selective reading behavior in Chronicle Wire.
 */
public class SkipsIgnoresEveryThingTest extends net.openhft.chronicle.wire.WireTestCommon {

    /**
     * Tests selective method invocation based on a condition. This test specifically focuses on
     * invoking different implementations based on an ID. It ensures that certain messages are skipped
     * based on the given condition.
     */
    @Test
    public void selective() {
        // Setup a wire with predefined text representing a sequence of messages
        String text = "to: 1\n" +
                "say: hi\n" +
                "...\n" +
                "to: 2\n" +
                "say: bad\n" +
                "...\n" +
                "to: 3\n" +
                "say: fine\n" +
                "...\n" +
                "to: 4\n" +
                "say: bad\n" +
                "...\n";

        Wire wire = new TextWire(Bytes.from(text)).useTextDocuments();
        List<String> words = new ArrayList<>();

        // Create a method reader that routes messages to different handlers based on the ID
        final MethodReader reader = wire.methodReader(new Selective() {
            final DontSayBad dsb = new DontSayBad();

            @Override
            public Saying to(long id) {
                // If the ID is even, use the DontSayBad handler, otherwise collect the words
                if (id % 2 == 0)
                    return dsb;
                return words::add;
            }
        });

        // Read messages and assert their processing
        for (int i = 4; i >= 0; i--)
            assertEquals(i > 0, reader.readOne());
        assertEquals("[hi, fine]", words.toString()); // Assert only valid words are collected
    }

    /**
     * Interface representing a selective method routing based on an ID.
     */
    interface Selective {
        Saying to(long id);
    }

    /**
     * Interface representing an action of saying a text.
     */
    interface Saying {
        void say(String text);
    }

    /**
     * Implementation of Saying that ignores messages containing "bad".
     * It also implements IgnoresEverything to signify it can ignore certain inputs.
     */
    static class DontSayBad implements Saying, IgnoresEverything {
        @Override
        public void say(String text) {
            // Assert that the text is not "bad"
            assertNotEquals("bad", text);
        }
    }
}
