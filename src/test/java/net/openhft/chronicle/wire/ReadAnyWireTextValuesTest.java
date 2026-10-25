/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class ReadAnyWireTextValuesTest extends WireAsTextTestCommon {

    @Parameterized.Parameters(name = "written as {0}")
    public static Collection<Object[]> writers() {
        return Arrays.asList(new Object[][]{
                {WireType.TEXT},
                {WireType.BINARY}
        });
    }

    @Parameterized.Parameter
    public WireType writer;

    @Override
    protected Wire wireWithText(String... values) {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Wire out = writer.apply(bytes);
        for (String value : values)
            out.write().text(value);
        return WireType.READ_ANY.apply(bytes);
    }
}
