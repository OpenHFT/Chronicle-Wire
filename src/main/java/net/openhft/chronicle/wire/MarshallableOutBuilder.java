//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//
package net.openhft.chronicle.wire;

import net.openhft.chronicle.wire.internal.FileMarshallableOut;
import net.openhft.chronicle.wire.internal.HTTPMarshallableOut;

import java.net.URL;
import java.util.function.Supplier;

/**
 * This is the {@code MarshallableOutBuilder} class.
 * It provides functionality to construct instances of {@link MarshallableOut} based on a specified URL.
 * The class follows the builder pattern, enabling the caller to set desired configurations and then retrieve
 * the appropriate {@code MarshallableOut} implementation based on the URL's protocol.
 */
public class MarshallableOutBuilder implements Supplier<MarshallableOut> {

    // The URL for which the MarshallableOut instance will be created.
    private final URL url;

    // The WireType configuration for the MarshallableOut.
    private WireType wireType;

    /**
     * Constructs a new {@code MarshallableOutBuilder} with the specified URL.
     *
     * @param url the URL for which the MarshallableOut instance will be created.
     */
    public MarshallableOutBuilder(URL url) {
        this.url = url;
    }

    @Override
    public MarshallableOut get() {
        switch (url.getProtocol()) {
            case "tcp":
                throw new UnsupportedOperationException("Direct TCP connection not implemented");
            case "file":
                if (wireType != null && wireType != WireType.YAML_ONLY)
                    throw new IllegalArgumentException("Unsupported wireType; " + wireType);
                // URL file protocol doesn't support writing...
                return new FileMarshallableOut(this, wireTypeOr(WireType.YAML_ONLY));
            case "http":
            case "https":
                if (wireType != null && wireType != WireType.JSON_ONLY)
                    throw new IllegalArgumentException("Unsupported wireType; " + wireType);
                return new HTTPMarshallableOut(this, wireTypeOr(WireType.JSON_ONLY));
            default:
                throw new UnsupportedOperationException("Writing to " + url.getProtocol() + " is  not implemented");
        }
    }

    /**
     * A helper method to determine the {@link WireType} based on current configuration and fallback option.
     *
     * @param wireType the fallback {@code WireType} to be used if none is set.
     * @return the currently set {@code WireType} or the provided fallback.
     */
    private WireType wireTypeOr(WireType wireType) {
        return this.wireType == null ? wireType : this.wireType;
    }

    /**
     * Returns the URL set for this builder.
     *
     * @return the set URL.
     */
    public URL url() {
        return url;
    }

    /**
     * Sets the desired {@link WireType} for the builder.
     * This method is part of the builder pattern allowing chained method calls.
     *
     * @param wireType the {@code WireType} to set.
     * @return the current instance of {@code MarshallableOutBuilder}.
     */
    public MarshallableOutBuilder wireType(WireType wireType) {
        this.wireType = wireType;
        return this;
    }
}
