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
 * Converts long values to a sequence of words and vice-versa. The conversion uses a predefined list of common words,
 * and each word is mapped to a unique segment of the long value.
 */
public class WordsLongConverter implements LongConverter {

    /** A regex pattern to match non-letter characters. */
    static final Pattern NON_LETTER = Pattern.compile("\\W");

    /** An array of words loaded from a file, used for conversions. */
    static final String[] WORDS;

    /**
     * A mapping of words to their respective indexes. Used to quickly find
     * a word's index, which is crucial for the conversion to long.
     */
    static final Map<String, Integer> WORD_ID = new HashMap<>();

    // Static block to initialize the WORDS array and the WORD_ID map.
    static {
        try {
            // Load the words from a resource file.
            String[] words = new String(IOTools.readFile(WordsLongConverter.class, "common-words.txt"), StandardCharsets.ISO_8859_1).split("\\s+");
            WORDS = words;

            // Populate the WORD_ID map.
            for (int i = 0; i < WORDS.length; i++) {
                String word = WORDS[i];
                Integer ii = WORD_ID.put(word, i);
                assert ii == null : "Duplicate " + word;
            }
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    /** The character used to separate words in the string representation. */
    private final String sep;

    /**
     * Default constructor. Uses a period as the default word separator.
     */
    public WordsLongConverter() {
        this('.');
    }

    /**
     * Constructor with a specified word separator.
     *
     * @param sep The character used to separate words.
     */
    public WordsLongConverter(char sep) {
        this.sep = Character.toString(sep);
    }

    /**
     * Parses the provided text to produce a long value.
     *
     * @param text The sequence of words to parse.
     * @return The long value corresponding to the given word sequence.
     * @throws IllegalArgumentException If a word in the sequence is not recognized.
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
     * Appends the word representation of the given long value to the provided StringBuilder.
     *
     * @param text The StringBuilder to append to.
     * @param value The long value to be converted and appended.
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
     * Appends the word representation of the given long value to the provided Bytes object.
     *
     * @param bytes The Bytes object to append to.
     * @param value The long value to be converted and appended.
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
