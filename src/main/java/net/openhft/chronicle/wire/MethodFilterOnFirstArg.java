/*
 * Copyright 2016-2025 chronicle.software
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
 * Functional interface for filtering method invocations by examining the first argument.
 * Implementors decide, based on that argument, whether the rest of the call should be ignored.
 * This can save processing time when not every event needs to be handled.
 *
 * @param <T> type of the first argument
 */
@FunctionalInterface
public interface MethodFilterOnFirstArg<T> {
    /**
     * Determines whether a method should be ignored based on its name and first argument.
     *
     * @param methodName the name of the method being evaluated
     * @param firstArg   the first argument passed to the method
     * @return {@code true} if the call should be skipped entirely because {@code firstArg}
     * meets certain criteria, {@code false} to process the method normally
     */
    boolean ignoreMethodBasedOnFirstArg(String methodName, T firstArg);
}
