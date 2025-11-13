/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.bytes.UpdateInterceptor;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.util.Mocker;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireTestCommon;
import net.openhft.chronicle.wire.WireType;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.Assert.*;

// Test class for verifying the behavior of a method writer with different argument types and update interceptor
// run with -DdumpCode to see the generated code
public class MethodWriter2Test extends WireTestCommon {

    // Test to verify that method calls with DTO arguments are allowed through
    @Test
    public void allowThrough() {
        ignoreException("Generated code to call updateInterceptor for public abstract void net.openhft.chronicle.wire.method.FundingListener.fundingPrimitive(int) will box and generate garbage");
        check(true, ARGUMENT.DTO);
    }

    // Test to verify that method calls with primitive arguments are allowed through
    @Test
    public void allowThroughPrimitive() {
        check(true, ARGUMENT.PRIMITIVE);
    }

    // Test to verify that method calls with no arguments are allowed through
    @Test
    public void allowThroughNoArg() {
        ignoreException("Generated code to call updateInterceptor for public abstract void net.openhft.chronicle.wire.method.FundingListener.fundingPrimitive(int) will box and generate garbage");
        check(true, ARGUMENT.NONE);
    }

    // Test to verify that method calls with DTO arguments are blocked
    @Test
    public void block() {
        check(false, ARGUMENT.DTO);
    }

    // Test to verify that method calls with primitive arguments are blocked
    @Test
    public void blockPrimitive() {
        check(false, ARGUMENT.PRIMITIVE);
    }

    // Test to verify that method calls with no arguments are blocked
    @Test
    public void blockNoArg() {
        check(false, ARGUMENT.NONE);
    }

    // Helper method to perform the test based on argument type and whether the method call is allowed
    private void check(boolean allowThrough, ARGUMENT argument) throws InvalidMarshallableException {
        Wire wire = WireType.BINARY.apply(Bytes.allocateElasticOnHeap());
        wire.usePadding(true);

        // UpdateInterceptor decides whether to allow or block a method call
        UpdateInterceptor ui = (methodName, t) -> allowThrough;
        FundingListener fundingListener = wire.methodWriterBuilder(FundingOut.class).updateInterceptor(ui).build();
        argument.accept(fundingListener);

        // Capture the output of the method calls
        List<String> output = new ArrayList<>();
        FundingListener listener = Mocker.intercepting(FundingListener.class, "", output::add);
        @NotNull MethodReader mr = wire.methodReader(listener);

        // Check if the method call is allowed or blocked as expected and verify the output
        if (allowThrough) {
            assertTrue(mr.readOne());
            assertEquals(1, output.size());
            assertEquals(argument.expected(), output.toString());
            assertFalse(mr.readOne());
        } else {
            assertFalse(mr.readOne());
            assertEquals(0, output.size());
        }
    }

    // Enum to define different argument types and their expected output
    enum ARGUMENT implements Consumer<FundingListener> {
        // Different cases for DTO, primitive, and no-arg method calls
        DTO {
            @Override
            public String expected() {
                // Expected output for DTO
                return "[funding[!net.openhft.chronicle.wire.method.Funding {\n" +
                        "  symbol: 0,\n" +
                        "  fr: NaN,\n" +
                        "  mins: 0\n" +
                        "}\n" +
                        "]]";
            }

            @Override
            public void accept(FundingListener fundingListener) {
                fundingListener.funding(new Funding());
            }
        },
        PRIMITIVE {
            @Override
            public String expected() {
                // Expected output for primitive argument
                return "[fundingPrimitive[42]]";
            }

            @Override
            public void accept(FundingListener fundingListener) {
                fundingListener.fundingPrimitive(42);
            }
        },
        NONE {
            @Override
            public String expected() {
                // Expected output for no-arg method
                return "[fundingNoArg[]]";
            }

            @Override
            public void accept(FundingListener fundingListener) {
                fundingListener.fundingNoArg();
            }
        };

        protected abstract String expected();
    }
}
