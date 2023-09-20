/*
 * Copyright 2016-2020 Chronicle Software
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
 * Wrapper exception used to encapsulate and propagate exceptions to high-level API calls
 * that arise from {@link ReadMarshallable#unexpectedField(Object, ValueIn)}.
 * Typically, this is thrown when an unexpected field is encountered during marshalling or
 * unmarshalling processes.
 *
 * @since 2023-09-11
 */
public class UnexpectedFieldHandlingException extends RuntimeException {

    /**
     * Constructs a new UnexpectedFieldHandlingException with the provided underlying cause.
     *
     * @param cause The root cause of this exception, typically originating from
     *              {@link ReadMarshallable#unexpectedField(Object, ValueIn)}.
     */
    public UnexpectedFieldHandlingException(Throwable cause) {
        super(cause);
    }
}
