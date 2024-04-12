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

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.StackTrace;

public class ErrorsImpl implements ErrorsIn {
    // Reference to an ErrorsOut instance
    private final ErrorsOut out;

    // Constructor initializing ErrorsImpl with an ErrorsOut instance
    public ErrorsImpl(ErrorsOut out) {
        this.out = out;
    }

    // Implementation of debug method from ErrorsIn interface
    @Override
    public void debug(String msg) {
        // Logging a debug message using Jvm utility
        Jvm.debug().on(getClass(), msg);
    }

    // Implementation of debugWithException method from ErrorsIn interface
    @Override
    public void debugWithException(String msg) {
        // Logging a debug message with a StackTrace exception using Jvm utility
        Jvm.debug().on(getClass(), msg, new StackTrace());
    }

    // Implementation of warn method from ErrorsIn interface
    @Override
    public void warn(String msg) {
        // Logging a warning message using Jvm utility
        Jvm.warn().on(getClass(), msg);
    }

    // Implementation of warnWithException method from ErrorsIn interface
    @Override
    public void warnWithException(String msg) {
        // Logging a warning message with a StackTrace exception using Jvm utility
        Jvm.warn().on(getClass(), msg, new StackTrace());
    }

    // Implementation of error method from ErrorsIn interface
    @Override
    public void error(String msg) {
        // Logging an error message using Jvm utility
        Jvm.error().on(getClass(), msg);
    }

    // Implementation of errorWithException method from ErrorsIn interface
    @Override
    public void errorWithException(String msg) {
        // Logging an error message with a StackTrace exception using Jvm utility
        Jvm.error().on(getClass(), msg, new StackTrace());
    }

    // Implementation of outError method from ErrorsIn interface
    @Override
    public void outError(String msg) {
        // Sending error message to the output
        out.error(msg);
    }

    // Implementation of throwException method from ErrorsIn interface
    @Override
    public void throwException(String msg) {
        // Throwing a runtime exception with the provided message
        throw new RuntimeException(msg);
    }

    // Implementation of throwError method from ErrorsIn interface
    @Override
    public void throwError(String msg) {
        // Throwing a custom AssertionError with the provided message
        throw new MyAssertionError(msg);
    }

    // Custom AssertionError class used within ErrorsImpl
    public static class MyAssertionError extends AssertionError {
        private static final long serialVersionUID = 0L;
        public MyAssertionError(String msg) {
            super(msg);
        }
    }
}
