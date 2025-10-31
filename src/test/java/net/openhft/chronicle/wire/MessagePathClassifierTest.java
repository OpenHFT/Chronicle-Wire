/*
 * Copyright 2016-2025 chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
