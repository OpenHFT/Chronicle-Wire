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

import java.time.Duration;

public class NanoDurationLongConverter implements LongConverter {

    @Override
    public long parse(CharSequence text) {
        final Duration parse = Duration.parse(text);
        return parse.getSeconds() * 1000_000_000 + parse.getNano();
    }

    @Override
    public void append(StringBuilder text, long value) {
        text.append(duration(value));
    }

    @Override
    public void append(Bytes<?> bytes, long value) {
        bytes.append(duration(value).toString());
    }

    private Duration duration(long value) {
        return Duration.ofSeconds(value / 1_000_000_000,
                value % 1_000_000_000);
    }
}
