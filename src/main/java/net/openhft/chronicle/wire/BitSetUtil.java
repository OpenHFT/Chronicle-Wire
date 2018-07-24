package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.Jvm;

import java.lang.reflect.Field;
import java.util.BitSet;

/**
 * Created by Rob Austin
 */
class BitSetUtil {

    private static Field wordsField;

    static {
        try {
            wordsField = BitSet.class.getDeclaredField("words");
            wordsField.setAccessible(true);
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

}
