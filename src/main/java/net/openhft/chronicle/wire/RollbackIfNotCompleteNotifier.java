/*
 * Copyright 2016-2020 chronicle.software
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
 * The RollbackIfNotCompleteNotifier interface defines methods to handle rollback scenarios
 * in the event of incomplete operations or messages. Implementations of this interface
 * can ensure data integrity and consistency by rolling back changes if an operation
 * does not complete successfully.
 */
public interface RollbackIfNotCompleteNotifier {
    /**
     * Rolls back the current operation if it is not complete. This method should be
     * invoked to ensure data consistency and to prevent partial updates. Implementations
     * may throw an UnsupportedOperationException if rollback is not supported.
     *
     * @throws UnsupportedOperationException if the rollback operation is not supported
     */
    default void rollbackIfNotComplete() {
        throw new UnsupportedOperationException(getClass().getName());
    }

    /**
     * Checks if the current writing operation is complete. This method can be used to
     * determine if it's safe to proceed with further operations or if a rollback is required.
     *
     * @return true if the current operation is complete, false otherwise
     */
    default boolean writingIsComplete() {
        return true;
    }
}
