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

import net.openhft.chronicle.core.io.IOTools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

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

    public WordsLongConverter() {
        this('.');
    }

    public WordsLongConverter(char sep) {
        this.sep = Character.toString(sep);
    }

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

    @Override
    public void append(StringBuilder text, long value) {
        String sep = "";
        do {
            text.append(sep);
            text.append(WORDS[(int) (value & 2047)]);
            value >>>= 11;
            sep = this.sep;
        } while (value > 0);
    }
}
