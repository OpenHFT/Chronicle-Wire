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
 * This is the WordsLongConverter class implementing the LongConverter interface.
 * The primary purpose of this class is to convert long numbers to their equivalent word representation using a predefined word list.
 * The word list is sourced from a 'common-words.txt' file associated with this class.
 *
 * For example, a long value might be represented by a sequence of words.
 */
public class WordsLongConverter implements LongConverter {

    // A pattern to match non-letter characters.
    static final Pattern NON_LETTER = Pattern.compile("\\W");

    // A static array of words loaded from the 'common-words.txt' file.
    static final String[] WORDS;

    // A static map to associate each word with a unique identifier.
    static final Map<String, Integer> WORD_ID = new HashMap<>();

    // Static block to load words from the 'common-words.txt' file into the WORDS array and the WORD_ID map.
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

    // The separator used between words in the word representation of long numbers.
    private final String sep;

    /**
     * Default constructor that initializes the WordsLongConverter with a dot ('.') as the default separator.
     */
    public WordsLongConverter() {
        this('.');
    }

    /**
     * Constructor that initializes the WordsLongConverter with a specified separator character.
     *
     * @param sep The separator character to use between words in the word representation.
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
