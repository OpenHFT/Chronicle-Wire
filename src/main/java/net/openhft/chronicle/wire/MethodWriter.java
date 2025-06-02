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
 * The {@code MethodWriter} interface defines the contract for classes that have the capability to
 * output their data in a marshallable format using the {@link MarshallableOut} interface.
 * Implementers of this interface are expected to provide the logic to transform or serialize their internal
 * state to a format supported by the {@link MarshallableOut} instance provided.
 */
public interface MethodWriter {

    /**
     * Transforms or serializes the internal state of the implementer to the provided
     * {@link MarshallableOut} instance. Implementers should handle the logic for
     * extracting their state and using the methods available on the {@code out} parameter
     * to output this state in the appropriate format.
     *
     * @param out The {@link MarshallableOut} instance to which the implementer's state should be written.
     */
    void marshallableOut(MarshallableOut out);
}
