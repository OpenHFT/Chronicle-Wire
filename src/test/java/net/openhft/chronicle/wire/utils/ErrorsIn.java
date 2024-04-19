/*
 * Copyright 2016-2022 chronicle.software
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

package net.openhft.chronicle.wire.utils;

// Interface defining methods for various error and logging actions
public interface ErrorsIn {
    // Method to log a debug message
    void debug(String msg);

    // Method to log a debug message along with an exception
    void debugWithException(String msg);

    // Method to log a warning message
    void warn(String msg);

    // Method to log a warning message along with an exception
    void warnWithException(String msg);

    // Method to output an error message
    void outError(String msg);

    // Method to log an error message
    void error(String msg);

    // Method to log an error message along with an exception
    void errorWithException(String msg);

    // Method to throw a generic exception with a message
    void throwException(String msg);

    // Method to throw a custom error with a message
    void throwError(String msg);
}
