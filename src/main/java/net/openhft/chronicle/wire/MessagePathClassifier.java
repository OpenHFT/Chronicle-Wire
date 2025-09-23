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
 * Classifies message paths based on the history of each message.
 * <p>
 * In systems where messages may traverse several hops, known routes can be
 * registered as patterns of source IDs. Each pattern is linked to a {@code pathId}
 * which can later be used for metrics, monitoring or routing decisions. The
 * longest matching pattern wins.
 * <p>
 * Implements {@link IntSupplier} so the current thread's
 * {@link MessageHistory} can be classified directly via {@link #getAsInt()}.
 */
public class MessagePathClassifier implements IntSupplier {

    /**
     * Patterns of source IDs for classification. A message history matches a
     * pattern when its source IDs end with the sequence.
     */
    private final List<int[]> sourcePattern = new ArrayList<>();

    /**
     * Path IDs corresponding to the patterns. Index {@code i} relates to
     * {@code sourcePattern.get(i)}.
     */
    private final List<Integer> pathIds = new ArrayList<>();

    /**
     * Registers a path ID for message histories that end with the supplied
     * sequence of source IDs.
     * <p>
     * This method enables the user to define how the classifier should categorize certain patterns
     * of message history.
     *
     * @param pathId  the identifier to associate with this pattern
     * @param sources the sequence of source IDs that must appear at the end of
     *                a message history
     * @return this classifier for chaining
     * @throws IllegalArgumentException if the same sequence is registered with a
     *                                  different {@code pathId}
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

    /**
     * Returns the path ID for the current thread's {@link MessageHistory}.
     */
    @Override
    public int getAsInt() {
        return pathFor(MessageHistory.get());
    }

    /**
     * Determines the {@code pathId} for the supplied history.
     * <p>
     * All registered patterns are checked and the longest matching suffix is
     * selected.
     *
     * @param messageHistory the history to classify
     * @return the matching path ID
     * @throws IllegalStateException if none of the patterns apply
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

    /**
     * Returns a readable representation of the classifier state.
     */
    @Override
    public String toString() {
        return "MessagePathClassifier{" +
                "sourcePattern=" + sourcePattern.stream().map(Arrays::toString).collect(Collectors.toList()) +
                ", pathIds=" + pathIds +
                '}';
    }
}
