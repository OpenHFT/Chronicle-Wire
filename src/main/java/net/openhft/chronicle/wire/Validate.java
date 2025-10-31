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
package net.openhft.chronicle.wire;

/**
 * Interface for validating objects.
 *
 * <p>Implementations ensure the state of an object meets its invariants or
 * preconditions. It is often used for DTOs or configuration objects to ensure
 * their state is consistent and valid before use or after deserialisation. See
 * {@link net.openhft.chronicle.core.io.ValidatableUtil} for utilities related
 * to validation.
 */
public interface Validate {

    /**
     * Validates the provided object.
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
