/*
 * Copyright 2016-2020 chronicle.software
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
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Jvm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * The {@code MessagePathClassifier} class is responsible for classifying message paths based on their histories.
 * This classifier allows messages to be grouped or identified based on patterns of their source IDs. It provides
 * mechanisms to associate a path ID with certain patterns of source IDs and to retrieve the appropriate path ID
 * for a given message history.
 * <p>
 * This class also implements {@link IntSupplier}, allowing the direct fetching of path ID for the current
 * {@link MessageHistory}.
 */
public class MessagePathClassifier implements IntSupplier {

    // Patterns of source IDs for classification.
    private final List<int[]> sourcePattern = new ArrayList<>();

    // Path IDs corresponding to the patterns.
    private final List<Integer> pathIds = new ArrayList<>();

    /**
     * Registers a path ID for message histories ending with a specific sequence of source IDs.
     * <p>
     * This method enables the user to define how the classifier should categorize certain patterns
     * of message history.
     *
     * @param pathId  A unique identifier for the message path.
     * @param sources An ordered array of source IDs representing a pattern in the message history.
     * @return The current instance of the  for chaining.
     */
    public MessagePathClassifier addPathForSourcesEnding(int pathId, int... sources) {
        OptionalInt duplicate = IntStream.range(0, sourcePattern.size())
                .filter(s -> Arrays.equals(sources, sourcePattern.get(s)))
                .findFirst();
        if (duplicate.isPresent()) {
            if (sources[duplicate.getAsInt()] == pathId) {
                Jvm.debug().on(getClass(), "Added pathId " + pathId + " more than once");
                return this;
            }
            throw new IllegalArgumentException("Duplicate entry for " + Arrays.toString(sources) + " for path " + duplicate.getAsInt() + " and " + pathId);
        }
        sourcePattern.add(sources);
        pathIds.add(pathId);
        return this;
    }

    @Override
    public int getAsInt() {
        return pathFor(MessageHistory.get());
    }

    /**
     * Determines the path ID for a specific {@link MessageHistory}.
     *
     * @param messageHistory The message history to classify.
     * @return The classified path ID for the given message history.
     */
    public int pathFor(MessageHistory messageHistory) {
        Integer pathId = null;
        int length = -1;
        for (int i = 0; i < sourcePattern.size(); i++) {
            int[] sourceIds = sourcePattern.get(i);
            if (messageHistory.sourceIdsEndsWith(sourceIds)) {
                Integer pathId2 = pathIds.get(i);
                if (sourceIds.length > length) {
                    pathId = pathId2;
                    length = sourceIds.length;
                }
            }
        }
        if (pathId == null)
            throw new IllegalStateException("Unable to classify the pathId for " + messageHistory);
        return pathId;
    }

    @Override
    public String toString() {
        return "MessagePathClassifier{" +
                "sourcePattern=" + sourcePattern.stream().map(Arrays::toString).collect(Collectors.toList()) +
                ", pathIds=" + pathIds +
                '}';
    }
}
