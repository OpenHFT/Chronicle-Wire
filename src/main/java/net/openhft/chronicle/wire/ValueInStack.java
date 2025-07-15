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
 * Represents a stack structure specifically designed to manage {@link ValueInState} instances.
 * The primary purpose of this class is to provide an organized way to manage and retrieve states
 * at different levels, and efficiently reuse them without constant instantiation.
 */
class ValueInStack {

    /**
     * A list of {@link ValueInState} objects that can be reused to reduce
     * allocation. Acts as the underlying storage for the stack.
     */
    final List<ValueInState> freeList = new ArrayList<>();

    // Represents the current level of the stack
    int level = 0;

    /**
     * Constructs a new ValueInStack and adds the first ValueInState to the free list.
     */
    public ValueInStack() {
        addOne();
    }

    /**
     * Resets the current level to the initial state and clears the state of the first ValueInState.
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
     * Retrieves the {@link ValueInState} at the current level. If none exists, new instances are added until one does.
     *
     * @return The ValueInState at the current stack level
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
