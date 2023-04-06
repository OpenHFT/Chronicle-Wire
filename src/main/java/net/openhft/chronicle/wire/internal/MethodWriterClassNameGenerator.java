//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.openhft.chronicle.wire.internal;

import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.wire.Base32LongConverter;
import net.openhft.chronicle.wire.LongConverter;
import net.openhft.chronicle.wire.WireType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class MethodWriterClassNameGenerator {
    private static final int MAXIMUM_CLASS_NAME_LENGTH = 255 - ".class".length();
    private static final LongConverter CLASSNAME_HASH_LONG_CONVERTER;
    private static final int MAX_LENGTH_OF_HASH;

    public MethodWriterClassNameGenerator() {
    }

    public @NotNull String getClassName(@NotNull Set<Class<?>> interfaces, @Nullable String genericEvent, boolean metaData, boolean intercepting, @NotNull WireType wireType, boolean verboseTypes) {
        StringBuilder sb = new StringBuilder();
        interfaces.forEach((i) -> {
            if (i.getEnclosingClass() != null) {
                sb.append(i.getEnclosingClass().getSimpleName());
            }

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
            long hashOfNonTruncatedClassName = Maths.hash64(sb);
            sb.delete(firstIndexTruncated, endOfInterfacesIndex);
            sb.insert(firstIndexTruncated, Base32LongConverter.INSTANCE.asText(hashOfNonTruncatedClassName));
        }

        return sb.toString();
    }

    private @NotNull String toFirstCapCase(@NotNull String name) {
        return Character.toUpperCase(name.charAt(0)) + name.substring(1).toLowerCase();
    }

    static {
        CLASSNAME_HASH_LONG_CONVERTER = Base32LongConverter.INSTANCE;
        MAX_LENGTH_OF_HASH = CLASSNAME_HASH_LONG_CONVERTER.asText(Long.MIN_VALUE).length();
    }
}
