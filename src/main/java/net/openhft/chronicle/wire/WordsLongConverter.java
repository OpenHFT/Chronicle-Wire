/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.IOTools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

/**
 * The {@code WordsLongConverter} class implements the LongConverter interface.
 * Its primary purpose is to convert long numbers into their equivalent word representation using a predefined word list.
 * <p>
 * The word list is sourced from the 'common-words.txt' file associated with this class.
 * <p>
 * For example, a long value might be represented by a sequence of words from this list.
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
            // Load the words from the resource file, ignoring comment lines.
            String content = new String(IOTools.readFile(WordsLongConverter.class, "common-words.txt"), ISO_8859_1);
            String[] lines = content.split("\\R");
            List<String> list = new ArrayList<>();
            for (String line : lines) {
                String t = line.trim();
                if (t.isEmpty() || t.startsWith("#"))
                    continue;
                list.add(t);
            }
            WORDS = list.toArray(new String[0]);

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
            text.append(asep).append(WORDS[(int) (value & 2047)]);
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
