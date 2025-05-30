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

import java.util.ArrayList;
import java.util.List;

/**
 * Package-private stack of {@link ValueInState} objects used by {@link ValueIn}
 * implementations when parsing nested structures. States are recycled to minimise
 * allocation.
 */
class ValueInStack {

    /** Pool of {@link ValueInState} instances available for reuse. */
    final List<ValueInState> freeList = new ArrayList<>();

    /** Current depth of the stack. */
    int level = 0;

    /**
     * Creates a new stack and pre-allocates a {@link ValueInState} for level {@code 0}.
     */
    public ValueInStack() {
        addOne();
    }

    /**
     * Resets to level {@code 0} and clears the root {@link ValueInState}.
     */
    public void reset() {
        level = 0;
        freeList.get(0).reset();
    }

    /**
     * Pushes a new level onto the stack, reusing an existing {@link ValueInState} if available.
     */
    public void push() {
        level++;
        if (freeList.size() > level) {
            freeList.get(level).reset();
        }
    }

    /**
     * Pops the current level from the stack.
     *
     * @throws IllegalStateException if the stack would underflow
     */
    public void pop() {
        if (level < 0)
            throw new IllegalStateException();
        level--;
    }

    /**
     * Returns the {@link ValueInState} for the current level, creating additional
     * instances as necessary.
     */
    public ValueInState curr() {
        while (freeList.size() <= level)
            addOne();
        return freeList.get(level);
    }

    /**
     * Allocates and stores a new {@link ValueInState} in the pool.
     */
    private void addOne() {
        freeList.add(new ValueInState());
    }
}
