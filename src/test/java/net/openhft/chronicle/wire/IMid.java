/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

// An interface defining a method to get the next interface `ILast` based on a long parameter
public interface IMid {
    ILast next(long x);
}
