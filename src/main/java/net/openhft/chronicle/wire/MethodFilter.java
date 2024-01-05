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
 * Represents a functional interface designed to determine if a specific method should be
 * handled based on its name and its first argument.
 * <p>
 * Implementors of this interface can define custom logic to decide whether a particular
 * method, given its name and first argument, should be processed or ignored. This mechanism
 * offers flexibility and allows users to have granular control over which methods are
 * to be handled under different circumstances.
 * </p>
 */
@FunctionalInterface
public interface MethodFilter {

    /**
     * Determines whether a specific method should be handled based on its name and its
     * first argument.
     *
     * @param method   The name of the method being evaluated.
     * @param firstArg The first argument passed to the method.
     *
     * @return true if the method should be handled, otherwise false.
     */
    boolean shouldHandleMessage(String method, Object firstArg);
}
