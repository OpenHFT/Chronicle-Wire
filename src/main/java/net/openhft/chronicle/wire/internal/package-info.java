//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//

/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
/**
 * Provides internal utility classes and implementation details for the Chronicle Wire library.
 * <p>
 * Classes here are not part of the public API and may change without notice. They underpin
 * wire implementations, marshalling and code generation.
 *
 * <p>
 *  Specifically, the following actions (including, but not limited to) are not allowed
 *  on internal classes and packages:
 *  <ul>
 *      <li>Casting to</li>
 *      <li>Reflection of any kind</li>
 *      <li>Explicit Serialize/deserialize</li>
 *  </ul>
 * <p>
 * @see net.openhft.chronicle.wire.Wire
 * @see net.openhft.chronicle.wire.Marshallable
 */
package net.openhft.chronicle.wire.internal;
