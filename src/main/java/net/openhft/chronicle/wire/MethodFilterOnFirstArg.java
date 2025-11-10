//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//
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
