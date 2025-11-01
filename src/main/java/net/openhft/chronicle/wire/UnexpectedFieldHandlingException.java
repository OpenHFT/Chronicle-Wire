/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

/**
 * Wrapper exception used to encapsulate and propagate exceptions to high-level API calls
 * that arise from {@link ReadMarshallable#unexpectedField(Object, ValueIn)}.
 * Typically, this is thrown when an unexpected field is encountered during marshalling or
 * unmarshalling processes.
 */
public class UnexpectedFieldHandlingException extends RuntimeException {
    private static final long serialVersionUID = 0L;

    /**
     * Constructs a new UnexpectedFieldHandlingException with the provided underlying cause.
     *
     * @param cause The root cause of this exception, often an exception thrown
     *              from within an implementation of
     *              {@link ReadMarshallable#unexpectedField(Object, ValueIn)}.
     */
    public UnexpectedFieldHandlingException(Throwable cause) {
        super(cause);
    }
}
