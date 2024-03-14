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
 * Represents a functional interface for checking header numbers against specific criteria.
 * <p>
 * This interface provides a contract for checking a given header number and its position.
 * Typically, implementations of this interface will contain logic to determine whether
 * the provided header number, in the context of its position are a valid combination.
 */
@FunctionalInterface
public interface HeadNumberChecker {

    /**
     * Checks whether the provided header number meets a certain condition in the context of its position.
     *
     * @param headerNumber The header number to be checked.
     * @param position The position or context associated with the header number.
     * @return {@code true} if the header number is valid; {@code false} otherwise.
     */
    boolean checkHeaderNumber(long headerNumber, long position);
}
