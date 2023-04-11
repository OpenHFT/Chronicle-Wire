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
import net.openhft.chronicle.core.onoes.ExceptionHandler;

public class ErrorsImpl implements ErrorsIn {
    private final ErrorsOut out;

    public ErrorsImpl(ErrorsOut out) {
        this.out = out;
    }

    @Override
    public void debug(String msg) {
        Jvm.debug().on(getClass(), msg);
    }

    @Override
    public void debugWithException(String msg) {
        Jvm.debug().on(getClass(), msg, new StackTrace());
    }

    @Override
    public void warn(String msg) {
        Jvm.warn().on(getClass(), msg);
    }

    @Override
    public void warnWithException(String msg) {
        Jvm.warn().on(getClass(), msg, new StackTrace());
    }

    @Override
    public void error(String msg) {
        Jvm.error().on(getClass(), msg);
    }

    @Override
    public void errorWithException(String msg) {
        Jvm.error().on(getClass(), msg, new StackTrace());
    }

    @Override
    public void outError(String msg) {
        out.error(msg);
    }

    @Override
    public void throwException(String msg) {
        throw new RuntimeException(msg);
    }

    @Override
    public void throwError(String msg) {
        throw new MyAssertionError(msg);
    }

    public static class MyAssertionError extends AssertionError {
        public MyAssertionError(String msg) {
            super(msg);
        }
    }
}
