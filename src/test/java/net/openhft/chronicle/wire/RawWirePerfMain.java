/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import org.jetbrains.annotations.NotNull;

class RawWirePerfMain extends WireTestCommon {

    public static void main(String[] args) {
        @NotNull BinaryWirePerfTest test = new BinaryWirePerfTest();
        test.wirePerf(-1, true, false, true);
        test.wirePerfInts(-1, true, false, true);
    }
}
