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
 * Represents a functional interface that provides a mechanism to filter methods based on their
 * first argument. This interface is designed for multi-argument method calls, allowing the user
 * to decide, based on the first argument, whether to ignore the rest of the method's arguments
 * and the method itself.
 * <p>
 * Implementors of this interface can define custom logic to determine if a specific method
 * should be ignored based on its first argument. This can be especially useful in scenarios
 * where performance is crucial, and not every method needs to be processed.
 *
 * @param <T> the type of the first argument that the method receives
 */
@FunctionalInterface
public interface MethodFilterOnFirstArg<T> {
    /**
     * Determines whether a method should be ignored based on its name and its first argument.
     *
     * @param methodName The name of the method being evaluated.
     * @param firstArg   The first argument passed to the method.
     *
     * @return true if the method should be ignored, otherwise false.
     */
    boolean ignoreMethodBasedOnFirstArg(String methodName, T firstArg);
}
