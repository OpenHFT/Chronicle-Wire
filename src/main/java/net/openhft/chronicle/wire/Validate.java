/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

/**
 * Interface for validating objects against state invariants and configuration preconditions.
 *
 * <p>Implementations ensure the state of an object meets its invariants or
 * preconditions. It is often used for DTOs or configuration objects to ensure
 * their state is consistent and valid before use or after deserialisation. See
 * {@link net.openhft.chronicle.core.io.ValidatableUtil} for utilities related
 * to validation.
 */
public interface Validate {

    /**
     * Validates the provided object against invariants and preconditions.
     *
     * <p>If validation fails, this method should throw a descriptive
     * {@link RuntimeException} such as {@link IllegalStateException} or a custom
     * validation exception.
     *
     * @param o The object to be validated. Implementations will typically cast
     *          this to their specific type.
     */
    void validate(Object o);
}
