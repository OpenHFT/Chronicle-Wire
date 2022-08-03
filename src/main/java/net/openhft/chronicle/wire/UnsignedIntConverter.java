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

@Deprecated(/* to be removed in x.25 */)
public class UnsignedIntConverter implements IntConverter {
    private static final long MASK_32 = 0xFFFF_FFFFL;

    @Override
    public int parse(CharSequence text) {
        return Integer.parseUnsignedInt(text.toString());
    }

    @Override
    public void append(StringBuilder text, int value) {
        AppendableUtil.append(text, value & MASK_32);
    }
}
