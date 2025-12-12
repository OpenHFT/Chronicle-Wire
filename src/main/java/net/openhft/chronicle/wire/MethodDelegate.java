/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

/**
 * Represents an interface that provides a mechanism for method delegation.
 * Implementors may set a delegate to which method invocations are forwarded,
 * allowing behaviour to be modified, augmented or decorated.
 * <p>
 * Typically, an instance implementing {@code MethodDelegate} and another target
 * interface (for example {@code MyService}) is generated dynamically (see
 * {@link GenerateMethodDelegate}). When methods of {@code MyService} are called
 * on the generated instance, they are forwarded to the {@code delegate} object
 * set via the {@link #delegate(Object)} method, provided that the delegate also
 * implements {@code MyService}.
 * <p>
 * The type parameter {@code <O>} specifies the type of the delegate object to
 * which methods will be forwarded.
 *
 * @param <O> delegate type
 */
public interface MethodDelegate<O> {

    /**
     * Sets the delegate to which method invocations will be forwarded.
     * <p>
     * This mechanism allows the delegation of method calls to an alternate implementation,
     * enabling behaviours such as logging, mocking or additional processing. The provided
     * {@code delegate} should typically implement the same business interface(s) as the
     * proxy that implements this {@code MethodDelegate} interface so that calls can be
     * successfully forwarded.
     *
     * @param delegate The target object of type {@code O} that will receive the
     *                 forwarded method calls.
     */
    void delegate(O delegate);
}
