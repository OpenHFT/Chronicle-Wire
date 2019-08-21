/*
 * Copyright (c) 2016-2019 Chronicle Software Ltd
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
