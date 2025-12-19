/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class WireStringArrayTestSupport {
    private WireStringArrayTestSupport() {
    }

    static void assertStringArrayRoundTrip(Supplier<Wire> wireSupplier) {
        @NotNull Wire wire = wireSupplier.get();
        wire.bytes().append('!').append(TestStringArray.class.getName()).append(" { strings: [ a, b, c ] }");

        TestStringArray sa = wire.getValueIn().object(TestStringArray.class);
        assertEquals("[a, b, c]", Arrays.toString(sa.strings));

        @NotNull Wire wire2 = wireSupplier.get();
        wire2.bytes().append('!').append(TestStringArray.class.getName()).append(" { strings: abc }");

        TestStringArray sa2 = wire2.getValueIn().object(TestStringArray.class);
        assertEquals("[abc]", Arrays.toString(sa2.strings));
    }

    static class TestStringArray implements Marshallable {
        String[] strings;
    }
}
