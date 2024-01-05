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

import net.openhft.chronicle.core.Jvm;

import java.lang.reflect.Field;
import java.util.BitSet;

/**
 * Utility class to access and manipulate the internals of the {@link BitSet} class.
 * This class provides methods to directly interact with the underlying word storage
 * and related metadata of a BitSet. It leverages reflection to access private fields
 * and therefore, should be used with caution.
 */
final class BitSetUtil {

    // Reflective field reference to the 'words' field in BitSet
    private static final Field wordsField;
    // Reflective field reference to the 'wordsInUse' field in BitSet
    private static final Field wordsInUse;
    // Reflective field reference to the 'sizeIsSticky' field in BitSet
    private static final Field sizeIsSticky;

    // Private constructor to prevent instantiation of utility class
    private BitSetUtil() {
    }

    // Static block to initialize reflective fields
    static {
        try {
            wordsField = BitSet.class.getDeclaredField("words");
            wordsInUse = BitSet.class.getDeclaredField("wordsInUse");
            sizeIsSticky = BitSet.class.getDeclaredField("sizeIsSticky");
            // Making the fields accessible for manipulation
            Jvm.setAccessible(wordsField);
            Jvm.setAccessible(wordsInUse);
            Jvm.setAccessible(sizeIsSticky);
        } catch (Exception e) {
            // Rethrow any caught exception for external handling
            throw Jvm.rethrow(e);
        }
    }

    /**
     * Retrieves the word from a {@link BitSet} at the given index.
     *
     * @param bs    The target BitSet.
     * @param index The index of the word to retrieve.
     * @return The word (as a long value) at the given index in the BitSet.
     */
    static long getWord(BitSet bs, int index) {
        try {
            long[] longs = (long[]) wordsField.get(bs);
            return longs[index];
        } catch (IllegalAccessException e) {
            // Rethrow any caught exception for external handling
            throw Jvm.rethrow(e);
        }
    }

    /**
     * Sets the underlying words of a {@link BitSet} and updates related metadata.
     *
     * @param using The BitSet to modify.
     * @param words The new words to set in the BitSet.
     * @return The modified BitSet.
     */
    static BitSet set(final BitSet using, final long[] words) {
        try {
            wordsField.set(using, words);
            wordsInUse.set(using, words.length);
            sizeIsSticky.set(using, false);
            return using;
        } catch (IllegalAccessException e) {
            // Rethrow any caught exception for external handling
            throw Jvm.rethrow(e);
        }
    }
}
