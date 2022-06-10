/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.internal.VanillaLongConverter;

public class Base85LongConverter implements LongConverter {

    public static final int MAX_LENGTH = 10;
    public static final Base85LongConverter INSTANCE = new Base85LongConverter();
    private static final String CHARS =
            "0123456789" +
                    ":;<=>?@" +
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZ_" +
                    "abcdefghijklmnopqrstuvwxyz" +
                    "\"#$%&'()*+,-./ ";
    private final VanillaLongConverter delegate = new VanillaLongConverter(CHARS);

    @Override
    public int maxParseLength() {
        return MAX_LENGTH;
    }

    @Override
    public long parse(CharSequence text) {
        return delegate.parse(text);
    }

    @Override
    public void append(StringBuilder text, long value) {
        delegate.append(text, value);
    }

    public void append(Bytes<?> text, long value) {
        delegate.append(text, value);
    }

    @Override
    public boolean allSafeChars() {
        return false;
    }
}
