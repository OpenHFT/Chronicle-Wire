/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

final class NestedSequenceSupport {
    private NestedSequenceSupport() {
    }

    @NotNull
    static List<List<Integer>> readNestedIntSequences(@NotNull WireIn wire, @NotNull String fieldName) {
        List<List<Integer>> result = new ArrayList<>();
        wire.read(fieldName).sequence(result, (list, outer) -> {
            while (outer.hasNextSequenceItem()) {
                List<Integer> inner = new ArrayList<>();
                outer.sequence(inner, (innerList, v) -> {
                    while (v.hasNextSequenceItem()) {
                        innerList.add(v.int32());
                    }
                });
                list.add(inner);
            }
        });
        return result;
    }
}
