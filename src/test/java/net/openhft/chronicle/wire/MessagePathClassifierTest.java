/*
 * Copyright (c) 2016-2019 Chronicle Software Ltd
 */

package net.openhft.chronicle.wire;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MessagePathClassifierTest extends WireTestCommon {

    private static VanillaMessageHistory messageHistory(String cs) {
        return Marshallable.fromString(VanillaMessageHistory.class, cs);
    }

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
        assertEquals(3, mpc.pathFor(messageHistory("sources: [ 3, 0 ]")));
        assertEquals(43, mpc.pathFor(messageHistory("sources: [ 4, 0, 3, 0 ]")));
        assertEquals(4, mpc.pathFor(messageHistory("sources: [ 3, 0, 4, 0 ]")));
        assertEquals(23, mpc.pathFor(messageHistory("sources: [ 2, 0, 3, 0 ]")));
        assertEquals(123, mpc.pathFor(messageHistory("sources: [ 1, 0, 2, 0, 3, 0 ]")));
        assertEquals(0, mpc.pathFor(messageHistory("sources: [ 1, 0, 2, 0, 5, 0 ]")));
    }

    @Test
    public void addPathForSourcesEnding() {
        new MessagePathClassifier()
                .addPathForSourcesEnding(4, 4)
                .addPathForSourcesEnding(123, 1, 2, 3)
                .addPathForSourcesEnding(4, 4); // warning.
    }

    @Test(expected = IllegalArgumentException.class)
    public void addPathForSourcesEnding2() {
        new MessagePathClassifier()
                .addPathForSourcesEnding(4, 4)
                .addPathForSourcesEnding(123, 1, 2, 3)
                .addPathForSourcesEnding(444, 4); // error.
    }
}