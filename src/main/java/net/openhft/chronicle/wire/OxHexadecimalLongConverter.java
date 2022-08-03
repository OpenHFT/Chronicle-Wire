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

import net.openhft.chronicle.bytes.Bytes;

/**
 * @deprecated Use the HexadecimalLongConverter
 */
@Deprecated(/* to be removed in x.25 */)
public class OxHexadecimalLongConverter implements LongConverter {

    public static final LongConverter INSTANCE = new OxHexadecimalLongConverter();

    @Override
    public long parse(CharSequence text) {
        String s = text.toString();
        if (s.startsWith("0x") || s.startsWith("0X"))
            s = s.substring(2);
        return Long.parseUnsignedLong(s, 16);
    }

    @Override
    public void append(StringBuilder text, long value) {
        text.append("0x").append(Long.toHexString(value));
    }

    @Override
    public void append(Bytes<?> bytes, long value) {
        bytes.append("0x").append(Long.toHexString(value));
    }
}
