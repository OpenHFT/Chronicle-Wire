/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

/**
 * Helper for mapping Java types to the method writer method names used when
 * generating proxies.
 */
final class MethodWriterTypeUtil {

    /**
     * Utility holder for writer type mapping; not instantiable.
     */
    private MethodWriterTypeUtil() {
    }

    /**
     * Maps a Java type to the writer alias used in text/binary outputs (e.g. {@code int} -> {@code int32}).
     *
     * @param type the Java parameter type
     * @return the writer method suffix representing that type
     */
    static CharSequence typeAlias(Class<?> type) {
        if (boolean.class.equals(type)) {
            return "bool";
        } else if (byte.class.equals(type)) {
            return "writeByte";
        } else if (char.class.equals(type)) {
            return "character";
        } else if (short.class.equals(type)) {
            return "int16";
        } else if (int.class.equals(type)) {
            return "int32";
        } else if (long.class.equals(type)) {
            return "int64";
        } else if (float.class.equals(type)) {
            return "float32";
        } else if (double.class.equals(type)) {
            return "float64";
        } else if (CharSequence.class.isAssignableFrom(type)) {
            return "text";
        } else if (Marshallable.class.isAssignableFrom(type)) {
            return "marshallable";
        }
        return "object";
    }
}
