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

import net.openhft.chronicle.bytes.AppendableUtil;
import net.openhft.chronicle.bytes.Bytes;

@Deprecated(/* to be removed in x.25*/)
public class UnsignedLongConverter implements LongConverter {

    @Override
    public long parse(CharSequence text) {
        return Long.parseUnsignedLong(text.toString());
    }

    @Override
    public void append(StringBuilder text, long value) {
        if (value >= 0)
            AppendableUtil.append(text, value);
        else
            text.append(Long.toUnsignedString(value));
    }

    @Override
    public void append(Bytes<?> bytes, long value) {
        if (value >= 0)
            AppendableUtil.append(bytes, value);
        else
            bytes.append(Long.toUnsignedString(value));
    }
}
