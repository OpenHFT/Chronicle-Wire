/*
 * Copyright 2016-2022 chronicle.software
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

/**
 * Represents an interface that provides a mechanism for method delegation.
 * Implementors of this interface can set a delegate to which method invocations are
 * forwarded. By using this interface, users can achieve behavior modification,
 * augmentation, or decoration of method calls by setting appropriate delegates.
 *
 * @param <OUT> The type of the delegate to which method calls will be forwarded.
 */
public interface MethodDelegate<OUT> {

    /**
     * Sets the delegate to which method invocations will be forwarded.
     * <p>
     * This mechanism allows the delegation of method calls to an alternate implementation,
     * enabling behaviors like logging, mocking, or additional processing.
     *
     * @param delegate The object that will receive the delegated method calls.
     */
    void delegate(OUT delegate);
}
