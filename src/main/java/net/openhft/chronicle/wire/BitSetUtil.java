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

import net.openhft.chronicle.core.Jvm;

import java.lang.reflect.Field;
import java.util.BitSet;

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
            throw Jvm.rethrow(e);
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
