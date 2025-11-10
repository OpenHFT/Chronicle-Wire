//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
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

    // A list of {@link ValueInState} objects that can be reused to reduce allocation. Acts as the underlying storage for the stack
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
