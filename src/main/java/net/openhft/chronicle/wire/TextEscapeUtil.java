/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

/**
 * Utilities for decoding escaped characters used in YAML/text wire formats.
 */
final class TextEscapeUtil {
    /**
     * Utility holder; not instantiable.
     */
    private TextEscapeUtil() {
    }

    /**
     * Decodes the escape sequence starting at the current index. The index reference is updated to
     * point at the last consumed character.
     */
    static char decodeEscapedChar(CharSequence seq, int[] indexRef) {
        int i = ++indexRef[0];
        char ch3 = seq.charAt(i);
        switch (ch3) {
            case '0':
                return 0;
            case 'a':
                return 7;
            case 'b':
                return '\b';
            case 't':
                return '\t';
            case 'n':
                return '\n';
            case 'v':
                return 0xB;
            case 'f':
                return 0xC;
            case 'r':
                return '\r';
            case 'e':
                return 0x1B;
            case 'N':
                return 0x85;
            case '_':
                return 0xA0;
            case 'L':
                return 0x2028;
            case 'P':
                return 0x2029;
            case 'x': {
                indexRef[0]++;
                char high = seq.charAt(indexRef[0]);
                indexRef[0]++;
                char low = seq.charAt(indexRef[0]);
                return (char) (Character.getNumericValue(high) * 16 + Character.getNumericValue(low));
            }
            case 'u': {
                indexRef[0]++;
                char b3 = seq.charAt(indexRef[0]);
                indexRef[0]++;
                char b2 = seq.charAt(indexRef[0]);
                indexRef[0]++;
                char b1 = seq.charAt(indexRef[0]);
                indexRef[0]++;
                char b0 = seq.charAt(indexRef[0]);
                return (char) (Character.getNumericValue(b3) * 4096 +
                        Character.getNumericValue(b2) * 256 +
                        Character.getNumericValue(b1) * 16 +
                        Character.getNumericValue(b0));
            }
            default:
                return ch3;
        }
    }
}
