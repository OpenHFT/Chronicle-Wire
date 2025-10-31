/*
 * Copyright 2016-2025 chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
