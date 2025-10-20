/*
 * Copyright 2016-2025 chronicle.software
 */

package net.openhft.chronicle.wire.internal;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.wire.Base32LongConverter;
import net.openhft.chronicle.wire.LongConverter;
import net.openhft.chronicle.wire.WireType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Internal helper that derives class names for generated method writer
 * implementations. The name reflects the implemented interfaces, selected
 * configuration flags and the {@link WireType} so that each combination maps
 * to a distinct class. Generated names are suitable for use as file names and
 * within a class loader cache.
 * <p>
 * This class is for internal use within Chronicle Wire and is not part of the
 * public API.
 */
public class MethodWriterClassNameGenerator {

    /**
     * Upper limit for the name length so that the compiled {@code .class} file
     * remains portable across file systems and JVMs.
     */
    private static final int MAXIMUM_CLASS_NAME_LENGTH = 255 - ".class".length();

    /**
     * Converter that yields a compact, filename-safe string representation of a
     * hash. The resulting text is appended to truncated names to preserve
     * uniqueness.
     */
    private static final LongConverter CLASSNAME_HASH_LONG_CONVERTER =
            Base32LongConverter.INSTANCE;

    /**
     * Maximum length of the text produced by
     * {@link #CLASSNAME_HASH_LONG_CONVERTER} for any long value.
     */
    private static final int MAX_LENGTH_OF_HASH =
            CLASSNAME_HASH_LONG_CONVERTER.asText(Long.MIN_VALUE).length();

    /**
     * Builds a class name from the supplied interfaces and options. The method
     * joins simplified interface names with any flags and the given
     * {@link WireType}, then appends {@code MethodWriter}. If the result exceeds
     * {@link #MAXIMUM_CLASS_NAME_LENGTH} the interface portion is shortened and a
     * Base32 hash of the full name is inserted to keep it unique.
     *
     * @param interfaces   interfaces the writer will implement
     * @param genericEvent name of the generic event method, or {@code null}
     * @param metaData     {@code true} if calls are written as meta-data
     * @param intercepting {@code true} when an interceptor is present
     * @param wireType     wire format of the writer
     * @param verboseTypes include verbose type information if {@code true}
     * @return the generated class name
     */
    @NotNull
    public String getClassName(@NotNull Set<Class<?>> interfaces,
                               @Nullable String genericEvent,
                               boolean metaData,
                               boolean intercepting,
                               @NotNull WireType wireType,
                               boolean verboseTypes) {

        final StringBuilder sb = new StringBuilder();

        interfaces.forEach(i -> {
            if (i.getEnclosingClass() != null)
                sb.append(i.getEnclosingClass().getSimpleName());
            sb.append(i.getSimpleName());
        });
        int endOfInterfacesIndex = sb.length();
        sb.append(genericEvent == null ? "" : genericEvent);
        sb.append(metaData ? "MetadataAware" : "");
        sb.append(intercepting ? "Intercepting" : "");
        sb.append(this.toFirstCapCase(wireType.toString().replace("_", "")));
        if (verboseTypes)
            sb.append("Verbose");

        sb.append("MethodWriter");
        if (sb.length() > MAXIMUM_CLASS_NAME_LENGTH) {
            int firstIndexTruncated = endOfInterfacesIndex - (sb.length() - MAXIMUM_CLASS_NAME_LENGTH) - MAX_LENGTH_OF_HASH;
            final long hashOfNonTruncatedClassName = Maths.hash64(sb);
            sb.delete(firstIndexTruncated, endOfInterfacesIndex);
            sb.insert(firstIndexTruncated, Base32LongConverter.INSTANCE.asText(hashOfNonTruncatedClassName));
        }
        return sb.toString();
    }

    /**
     * Converts the supplied text to first capital case. Only the first
     * character remains as-is; the rest is converted to lower case. The input is
     * expected to contain no underscores as the caller removes them.
     */
    @NotNull
    private String toFirstCapCase(@NotNull String name) {
        return Character.toUpperCase(name.charAt(0)) + name.substring(1).toLowerCase();
    }
}
