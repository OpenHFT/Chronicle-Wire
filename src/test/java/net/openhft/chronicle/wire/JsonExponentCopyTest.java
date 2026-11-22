/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import static org.junit.Assert.*;

import net.openhft.chronicle.bytes.*;

import org.junit.Test;

import java.lang.annotation.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;

public class JsonExponentCopyTest extends WireTestCommon {

    private void copyNumber(String token, double expected, boolean text) {
        Bytes<?> input = Bytes.from("{\"value\":" + token + ",\"tail\":7}");
        Bytes<?> output = Bytes.allocateElasticOnHeap();
        try {
            JSONWire json = new JSONWire(input);
            Wire target = text ? new TextWire(output) : new BinaryWire(output);
            long limit = input.readLimit();
            json.copyTo(target);
            Map<?, ?> result = target.getValueIn().object(Map.class);
            assertEquals(expected, ((Number) result.get("value")).doubleValue(), 0);
            assertEquals(7, ((Number) result.get("tail")).intValue());
            assertEquals(limit, input.readLimit());
        } finally {
            output.releaseLast();
            input.releaseLast();
        }
    }

    @Test
    public void lowercaseExponentCopiesToBinary() throws Exception {
        copyNumber("1e3", 1000, false);
    }

    @Test
    public void uppercaseExponentCopiesToBinary() throws Exception {
        copyNumber("2E2", 200, false);
    }

    @Test
    public void negativeExponentCopiesToBinary() throws Exception {
        copyNumber("12.5e-2", 0.125, false);
    }

    @Test
    public void signedExponentCopiesToText() throws Exception {
        copyNumber("2e+2", 200, true);
    }

    @Test
    public void integerCopyPreservesNextField() throws Exception {
        copyNumber("-42", -42, false);
    }

    @Test
    public void decimalCopyPreservesNextField() throws Exception {
        copyNumber("1.25", 1.25, false);
    }
}
