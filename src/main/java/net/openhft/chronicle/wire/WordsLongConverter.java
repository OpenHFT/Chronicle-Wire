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
import net.openhft.chronicle.core.io.IOTools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A converter for long values into words and vice versa. The converter
 * uses a set of common words for encoding and decoding operations.
 */
public class WordsLongConverter implements LongConverter {
    static final Pattern NON_LETTER = Pattern.compile("\\W");
    static final String[] WORDS;
    static final Map<String, Integer> WORD_ID = new HashMap<>();

    static {
        try {
            String[] words = new String(IOTools.readFile(WordsLongConverter.class, "common-words.txt"), StandardCharsets.ISO_8859_1).split("\\s+");
            WORDS = words;
            for (int i = 0; i < WORDS.length; i++) {
                String word = WORDS[i];
                Integer ii = WORD_ID.put(word, i);
                assert ii == null : "Duplicate " + word;
            }
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private final String sep;

    /**
     * Default constructor which uses '.' as separator.
     */
    public WordsLongConverter() {
        this('.');
    }

    /**
     * Initializes a new converter with the given separator.
     *
     * @param sep the separator to use for encoding and decoding
     */
    public WordsLongConverter(char sep) {
        this.sep = Character.toString(sep);
    }

    /**
     * Parses a sequence of characters into a long value.
     *
     * @param text the character sequence to parse
     * @return the parsed long value
     * @throws IllegalArgumentException if the character sequence contains an unknown word
     */
    @Override
    public long parse(CharSequence text) {
        String[] split = NON_LETTER.split(text.toString().trim(), 0);
        long value = 0;
        int shift = 0;
        for (String s : split) {
            Integer id = WORD_ID.get(s);
            if (id == null)
                throw new IllegalArgumentException("Unknown word'" + s + "'");
            value += id.longValue() << shift;
            shift += 11;
        }
        return value;
    }

    /**
     * Appends a long value to a StringBuilder.
     *
     * @param text the StringBuilder to append to
     * @param value the long value to append
     */
    @Override
    public void append(StringBuilder text, long value) {
        String asep = "";
        do {
            text.append(asep);
            text.append(WORDS[(int) (value & 2047)]);
            value >>>= 11;
            asep = this.sep;
        } while (value > 0);
    }

    /**
     * Appends a long value to a Bytes object.
     *
     * @param bytes the Bytes object to append to
     * @param value the long value to append
     */
    @Override
    public void append(Bytes<?> bytes, long value) {
        String asep = "";
        do {
            bytes.append(asep);
            bytes.append(WORDS[(int) (value & 2047)]);
            value >>>= 11;
            asep = this.sep;
        } while (value > 0);
    }
}
