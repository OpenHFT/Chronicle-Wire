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

public class WatermarkedMicroTimestampLongConverter extends MicroTimestampLongConverter {
    @Override
    public long parse(CharSequence text) {
        char letter = text.charAt(0);
        if (('A' <= letter && letter <= 'Z') || ('a' <= letter && letter <= 'z')) {
            int digit = text.charAt(1);
            if ('0' <= digit && digit <= '9') {
                long watermark = (long) (((letter & 0x31) - 1) * 10 + digit) << -8;
                long parse = super.parse(text.subSequence(2, text.length()));
                return parse | watermark;
            }
        }
        throw new IllegalArgumentException("Unknown watermark");
    }

    @Override
    public void append(StringBuilder text, long value) {
        long time = value & (~0L << -8);
        long watermark = value >>> -8;
        text.append((char) ('A' + watermark / 10));
        text.append(watermark % 10);
        super.append(text, time);
    }
}
