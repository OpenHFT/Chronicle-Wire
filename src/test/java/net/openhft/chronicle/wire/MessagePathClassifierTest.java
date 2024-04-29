/*
 * Copyright (c) 2016-2020 chronicle.software
 */

package net.openhft.chronicle.wire;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("deprecation")
public class MessagePathClassifierTest extends WireTestCommon {

    // Utility function to convert a string into a VanillaMessageHistory object.
    private static VanillaMessageHistory messageHistory(String cs) {
        return Marshallable.fromString(VanillaMessageHistory.class, cs);
    }

    // Test that the pathFor method correctly identifies the path ID of a message based on its sources.
    @Test
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
        assertEquals(3, mpc.pathFor(messageHistory("sources: [ 3, 0 ]")));
        assertEquals(43, mpc.pathFor(messageHistory("sources: [ 4, 0, 3, 0 ]")));
        assertEquals(4, mpc.pathFor(messageHistory("sources: [ 3, 0, 4, 0 ]")));
        assertEquals(23, mpc.pathFor(messageHistory("sources: [ 2, 0, 3, 0 ]")));
        assertEquals(123, mpc.pathFor(messageHistory("sources: [ 1, 0, 2, 0, 3, 0 ]")));
        assertEquals(0, mpc.pathFor(messageHistory("sources: [ 1, 0, 2, 0, 5, 0 ]")));
    }

    // Test the toString method of MessagePathClassifier to ensure it correctly displays source patterns and path IDs.
    @Test
    public void addPathForSourcesEnding() {
        final MessagePathClassifier mpc = new MessagePathClassifier()
                .addPathForSourcesEnding(4, 4)
                .addPathForSourcesEnding(123, 1, 2, 3)
                .addPathForSourcesEnding(4, 4);
        assertEquals("" +
                        "MessagePathClassifier{sourcePattern=[[4], [1, 2, 3]], pathIds=[4, 123]}",
                mpc.toString());
    }

    // Test for an exception to be thrown when a source pattern is duplicated in MessagePathClassifier.
    @Test(expected = IllegalArgumentException.class)
    public void addPathForSourcesEnding2() {
        new MessagePathClassifier()
                .addPathForSourcesEnding(4, 4)
                .addPathForSourcesEnding(123, 1, 2, 3)
                .addPathForSourcesEnding(444, 4); // error. Expected to throw IllegalArgumentException
    }
}
