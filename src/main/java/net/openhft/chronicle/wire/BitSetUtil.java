package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Jvm;

import java.lang.reflect.Field;
import java.util.BitSet;

/**
 * Created by Rob Austin
 */
class BitSetUtil {

    private static Field wordsField;
    private static Field wordsInUse;
    private static Field sizeIsSticky;

    static {
        try {
            wordsField = BitSet.class.getDeclaredField("words");
            wordsField.setAccessible(true);
            wordsInUse = BitSet.class.getDeclaredField("wordsInUse");
            wordsInUse.setAccessible(true);
            sizeIsSticky = BitSet.class.getDeclaredField("sizeIsSticky");
            sizeIsSticky.setAccessible(true);
        } catch (Exception e) {
            Jvm.rethrow(e);
        }
    }

    static long getWord(BitSet bs, int index) {
        try {
            long[] longs = (long[]) wordsField.get(bs);
            return longs[index];
        } catch (IllegalAccessException e) {
            throw Jvm.rethrow(e);
        }
    }

    static <E> BitSet set(final BitSet using, final long[] words) {
        try {
            wordsField.set(using, words);
            wordsInUse.set(using, words.length);
            sizeIsSticky.set(using, false);
            return using;
        } catch (IllegalAccessException e) {
            throw Jvm.rethrow(e);
        }
    }
}
