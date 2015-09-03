/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.pool.StringBuilderPool;
import org.jetbrains.annotations.NotNull;

/**
 * Created by peter on 31/08/15.
 */
public enum Wires {
    ;
    static final int LENGTH_MASK = -1 >>> 2;
    static final StringBuilderPool SBP = new StringBuilderPool();
    static final int NOT_READY = 1 << 31;
    static final int META_DATA = 1 << 30;
    static final int UNKNOWN_LENGTH = 0x0;

    /**
     * This decodes some Bytes where the first 4-bytes is the length.  e.g. Wire.writeDocument wrote it.
     * <a href="https://github.com/OpenHFT/RFC/tree/master/Size-Prefixed-Blob">Size Prefixed Blob</a>
     *
     * @param bytes to decode
     * @return as String
     */
    public static String fromSizePrefixedBlobs(@NotNull Bytes bytes) {
        long position = bytes.readPosition();
        return WireInternal.fromSizePrefixedBlobs(bytes, position, bytes.readRemaining());
    }

    public static StringBuilder acquireStringBuilder() {
        return SBP.acquireStringBuilder();
    }

    public static int lengthOf(long len) {
        return (int) (len & LENGTH_MASK);
    }

    public static boolean isReady(long len) {
        return (len & NOT_READY) == 0;
    }

    public static boolean isData(long len) {
        return (len & META_DATA) == 0;
    }
}
