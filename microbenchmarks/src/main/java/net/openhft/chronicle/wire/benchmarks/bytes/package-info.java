/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
/**
 * Byte-level benchmarks for Chronicle Wire data layouts.
 *
 * <p>Code here models DTOs directly over {@code BytesStore} to measure native
 * access patterns, allocation behaviour, and serialization performance.
 */
package net.openhft.chronicle.wire.benchmarks.bytes;
