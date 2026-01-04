/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("deprecation")
public class MessagePathClassifierTest extends WireTestCommon {

    // Utility function to convert a string into a VanillaMessageHistory object.
    private static VanillaMessageHistory messageHistory(String cs) {
        return Marshallable.fromString(VanillaMessageHistory.class, cs);
    }

    // Test that the pathFor method correctly identifies the path ID of a message based on its sources.
    @Test
    @DisplayName("Path ids resolve from source suffixes")
    public void pathFor() {
        MessagePathClassifier mpc =
                new MessagePathClassifier()
                        .addPathForSourcesEnding(4, 4)
                        .addPathForSourcesEnding(123, 1, 2, 3)
                        .addPathForSourcesEnding(23, 2, 3)
                        .addPathForSourcesEnding(43, 4, 3)
                        .addPathForSourcesEnding(3, 3)
                        .addPathForSourcesEnding(0);

        // Testing various sources and ensuring they match the expected path ID.
        assertEquals(3, mpc.pathFor(messageHistory("sources: [ 3, 0 ]")),
                "Path id should be 3 for sources [3, 0]");
        assertEquals(43, mpc.pathFor(messageHistory("sources: [ 4, 0, 3, 0 ]")),
                "Path id should be 43 for sources [4, 0, 3, 0]");
        assertEquals(4, mpc.pathFor(messageHistory("sources: [ 3, 0, 4, 0 ]")),
                "Path id should be 4 for sources [3, 0, 4, 0]");
        assertEquals(23, mpc.pathFor(messageHistory("sources: [ 2, 0, 3, 0 ]")),
                "Path id should be 23 for sources [2, 0, 3, 0]");
        assertEquals(123, mpc.pathFor(messageHistory("sources: [ 1, 0, 2, 0, 3, 0 ]")),
                "Path id should be 123 for sources [1, 0, 2, 0, 3, 0]");
        assertEquals(0, mpc.pathFor(messageHistory("sources: [ 1, 0, 2, 0, 5, 0 ]")),
                "Path id should be 0 for sources with no matching suffix");
    }

    // Test the toString method of MessagePathClassifier to ensure it correctly displays source patterns and path IDs.
    @Test
    @DisplayName("Classifier toString de-duplicates source patterns")
    public void addPathForSourcesEnding() {
        final MessagePathClassifier mpc = new MessagePathClassifier()
                .addPathForSourcesEnding(4, 4)
                .addPathForSourcesEnding(123, 1, 2, 3)
                .addPathForSourcesEnding(4, 4);
        assertEquals("MessagePathClassifier{sourcePattern=[[4], [1, 2, 3]], pathIds=[4, 123]}",
                mpc.toString(),
                "toString should report unique source patterns and ids");
    }

    // Test for an exception to be thrown when a source pattern is duplicated in MessagePathClassifier.
    @Test
    @DisplayName("Duplicate source pattern rejects conflicting id")
    public void addPathForSourcesEnding2() {
        assertThrows(IllegalArgumentException.class, () ->
                new MessagePathClassifier()
                        .addPathForSourcesEnding(4, 4)
                        .addPathForSourcesEnding(123, 1, 2, 3)
                        .addPathForSourcesEnding(444, 4),
                "Duplicate source pattern should be rejected");
    }
}
