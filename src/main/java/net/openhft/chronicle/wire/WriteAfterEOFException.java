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
 * This is the WriteAfterEOFException class extending IllegalStateException.
 * The exception is thrown when there's an attempt to write data after the End-Of-File (EOF) marker.
 * This is typically used to safeguard against improper file manipulation and to maintain data integrity.
 *
 * @since 2023-08-29
 */
public class WriteAfterEOFException extends IllegalStateException {

    /**
     * Constructs a new instance of WriteAfterEOFException with a default error message.
     * The message indicates that writing after EOF is not permitted.
     */
    public WriteAfterEOFException() {
        super("You should not be able to write at EOF");
    }
}
