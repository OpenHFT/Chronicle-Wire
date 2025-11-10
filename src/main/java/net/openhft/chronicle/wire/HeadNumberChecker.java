//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//
package net.openhft.chronicle.wire;

/**
 * Represents a functional interface for checking header numbers against specific criteria.
 * <p>
 * This interface provides a contract for checking a given header number and its position.
 * Typically, implementations of this interface will contain logic to determine whether
 * the provided header number, in the context of its position are a valid combination.
 * <p>
 * Used by {@link AbstractWire} to validate header numbers, for example, to ensure sequence
 * numbers are contiguous or monotonically increasing when reading from a queue.
 */
@FunctionalInterface
public interface HeadNumberChecker {

    /**
     * Checks whether the provided header number meets a certain condition in the context of its position.
     *
     * @param headerNumber The header number read from the wire.
     * @param position     The byte position in the wire where this header number was encountered.
     *                     This can be used for context or logging.
     * @return {@code true} if the header number is considered valid according to the implementation's
     * logic, {@code false} otherwise (which might lead to an error or retry).
     */
    boolean checkHeaderNumber(long headerNumber, long position);
}
