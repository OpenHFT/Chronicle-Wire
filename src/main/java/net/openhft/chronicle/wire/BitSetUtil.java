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
            // CSReflectiveFieldLookup REVIEW wordsField = BitSet.class.getDeclaredField("words") because this reflective or runtime-loading boundary still needs either an allowlisted wrapper or an explicit reviewed runtime-loading contract.
            wordsField = BitSet.class.getDeclaredField("words");
            // CSReflectiveFieldLookup REVIEW wordsInUse = BitSet.class.getDeclaredField("wordsInUse") because this reflective or runtime-loading boundary.
            wordsInUse = BitSet.class.getDeclaredField("wordsInUse");
            // CSReflectiveFieldLookup REVIEW sizeIsSticky = BitSet.class.getDeclaredField("sizeIsSticky") because this reflective or runtime-loading boundary.
            sizeIsSticky = BitSet.class.getDeclaredField("sizeIsSticky");
            // Making the fields accessible for manipulation
            // CSSetAccessibleEscalation REVIEW Jvm.setAccessible(wordsField) because this access override still needs either encapsulation-preserving access or an explicit reviewed runtime-access contract.
            Jvm.setAccessible(wordsField);
            // CSSetAccessibleEscalation REVIEW Jvm.setAccessible(wordsInUse) because this access override.
            Jvm.setAccessible(wordsInUse);
            // CSSetAccessibleEscalation REVIEW Jvm.setAccessible(sizeIsSticky) because this access override.
            Jvm.setAccessible(sizeIsSticky);
        } catch (Exception e) {
            // Rethrow any caught exception for external handling
            // CSCheckedSwallowThroughRethrow REVIEW throw Jvm.rethrow(e) because this rethrow converts a checked cause into an unchecked wrapper and still needs either a declared `throws` at the enclosing method or an explicit reviewed note on why no local cleanup is performed.
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
            // CSCheckedSwallowThroughRethrow REVIEW throw Jvm.rethrow(e) because this rethrow in BitSetUtil#getWord converts a checked cause into an unchecked wrapper and.
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
            // CSCheckedSwallowThroughRethrow REVIEW throw Jvm.rethrow(e) because this rethrow in BitSetUtil#set converts a checked cause into an unchecked wrapper and.
            throw Jvm.rethrow(e);
        }
    }
}
