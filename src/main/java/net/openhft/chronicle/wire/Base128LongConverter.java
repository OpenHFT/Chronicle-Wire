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
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.util.StringUtils;

/**
 * @deprecated This doesn't really work as it uses characters that have to be escaped out in text wires
 */
@Deprecated(/* use LongConversion, to be removed in x.25 */)
public class Base128LongConverter implements LongConverter {
    public static final int MAX_LENGTH = LongConverter.maxParseLength(128);
    public static final Base128LongConverter INSTANCE = new Base128LongConverter();

    @Override
    public int maxParseLength() {
        return MAX_LENGTH;
    }

    @Override
    public long parse(CharSequence text) {
        lengthCheck(text);
        long v = 0;
        for (int i = 0; i < text.length(); i++)
            v = (v << 7) + text.charAt(i);
        return v;
    }

    @Override
    public void append(StringBuilder text, long value) {
        int start = text.length();
        while (value != 0) {
            text.append((char) (value & 0x7F));
            value >>>= 7;
        }
        StringUtils.reverse(text, start);

        if (text.length() > start + maxParseLength()) {
            Jvm.warn().on(getClass(), "truncated because the value was too large");
            text.setLength(start + maxParseLength());
        }
    }

    @Override
    public void append(Bytes<?> bytes, long value) {
        StringBuilder sb = WireInternal.acquireStringBuilder();
        append(sb, value);
        bytes.append(sb);
    }

    @Override
    public boolean allSafeChars(@NotNull WireOut wireOut) {
        return false;
    }
}
