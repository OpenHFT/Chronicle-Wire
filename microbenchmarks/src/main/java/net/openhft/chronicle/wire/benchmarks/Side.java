/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.benchmarks;

/**
 * Trading side used in benchmark DTOs.
 * <p>
 * Encoded as a single character or boolean in some native layouts for compactness.
 */
public enum Side {
    Buy, Sell
}
