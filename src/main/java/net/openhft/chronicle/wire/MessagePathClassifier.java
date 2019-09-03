/*
 * Copyright (c) 2016-2019 Chronicle Software Ltd
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Jvm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.IntSupplier;
import java.util.stream.IntStream;

public class MessagePathClassifier implements IntSupplier {
    private final List<int[]> sourcePattern = new ArrayList<>();
    private final List<Integer> pathIds = new ArrayList<>();

    /**
     * @param pathId  0 indexed pathId for sourcces ending with
     * @param sources match a message history ending with this.
     * @return this
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
}
