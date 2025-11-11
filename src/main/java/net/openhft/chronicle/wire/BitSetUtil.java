/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Jvm;

import java.lang.reflect.Field;
import java.util.BitSet;

/**
 * Utility class to access and manipulate the internals of the {@link BitSet} class.
 * <p>
 * This class provides methods to directly interact with the underlying word storage
 * and related metadata of a BitSet. It leverages reflection to access private fields.
 * <b>This class relies on reflection of private fields of {@link java.util.BitSet}
 * and may break with different Java versions or JDK implementations. Use with extreme
 * caution and only when direct access to {@code BitSet} internals is absolutely necessary.</b>
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

    // Static block to initialise reflective fields
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
     * @param bs    The {@link java.util.BitSet} instance from which to retrieve the word.
     * @param index The zero-based index of the internal {@code long} word to retrieve.
     * @return The {@code long} value of the word at the specified {@code index}.
     * @throws RuntimeException wrapping {@link IllegalAccessException} if reflective access fails.
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
     * Directly sets the internal 'words' array of the {@code using} {@link java.util.BitSet}
     * to the provided {@code words} array. Also updates the 'wordsInUse' field to
     * {@code words.length} and 'sizeIsSticky' to {@code false} to reflect the change.
     *
     * @param using  The {@link java.util.BitSet} instance to modify.
     * @param words  The new {@code long[]} array to set as the internal bit storage.
     * @return The modified {@code using} {@link java.util.BitSet} instance.
     * @throws RuntimeException wrapping {@link IllegalAccessException} if reflective access fails.
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
