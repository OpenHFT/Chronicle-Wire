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
            wordsInUse = BitSet.class.getDeclaredField("wordsInUse");
            sizeIsSticky = BitSet.class.getDeclaredField("sizeIsSticky");
            Jvm.setAccessible(wordsField);
            Jvm.setAccessible(wordsInUse);
            Jvm.setAccessible(sizeIsSticky);
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

    static BitSet set(final BitSet using, final long[] words) {
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
