/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

final class YamlSpecificationSupport {
    private YamlSpecificationSupport() {
    }

    @NotNull
    static byte[] readSpecBytes(String file) throws IOException {
        try (InputStream is = YamlSpecificationSupport.class.getResourceAsStream("/yaml/spec/" + file)) {
            if (is == null)
                throw new IOException("Missing YAML specification resource: " + file);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = is.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }
}
