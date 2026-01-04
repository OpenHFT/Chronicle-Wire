/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.examples;

/**
 * Defines example operations for handling {@link Person} instances in wire demos.
 */
public interface PersonOps {

    /**
     * Add a new {@link Person} entry to the example sink.
     *
     * @param p person instance to add
     */
    void addPerson(Person p);
}
