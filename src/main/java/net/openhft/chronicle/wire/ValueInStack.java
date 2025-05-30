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
 * Package-private class that manages a stack of {@link ValueInState} objects.
 * Used by {@link ValueIn} implementations when reading nested data structures.
 */
class ValueInStack {

    /**
     * A list of {@link ValueInState} objects that can be reused to reduce
     * allocation. Acts as the underlying storage for the stack.
     */
    final List<ValueInState> freeList = new ArrayList<>();

    /** The current depth or level of the stack. {@code level = 0} is the base. */
    int level = 0;

    /**
     * Initialises the stack and pre-allocates the first {@link ValueInState} at level 0.
     */
    public ValueInStack() {
        addOne();
    }

    /**
     * Resets the stack to its initial state: {@link #level} is set to 0 and the
     * root {@link ValueInState} is reset.
     */
    public void reset() {
        level = 0;
        freeList.get(0).reset();
    }

    /**
     * Increments {@link #level}. If a state object already exists at the new
     * level it is reset for reuse.
     */
    public void push() {
        level++;
        if (freeList.size() > level) {
            freeList.get(level).reset();
        }
    }

    /**
     * Decrements {@link #level}. Throws {@link IllegalStateException} if called
     * below level&nbsp;0.
     */
    public void pop() {
        if (level < 0)
            throw new IllegalStateException();
        level--;
    }

    /**
     * Returns the {@link ValueInState} for the current {@link #level}. Extra
     * entries are added to {@link #freeList} if needed.
     */
    public ValueInState curr() {
        while (freeList.size() <= level)
            addOne();
        return freeList.get(level);
    }

    /**
     * Adds a new, default-initialised {@link ValueInState} to the free list.
     */
    private void addOne() {
        freeList.add(new ValueInState());
    }
}
