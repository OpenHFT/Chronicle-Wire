/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.core.util.Mocker;
import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

import static org.easymock.EasyMock.*;
import static org.junit.jupiter.api.Assertions.*;

public class ClassAliasPoolTest extends WireTestCommon {

    // Helper method to match char sequences in a mock setup
    private static CharSequence charSequence(String text) {
        EasyMock.reportMatcher(new IArgumentMatcher() {
            @Override
            public boolean matches(Object argument) {
                return argument.toString().equals(text);
            }

            @Override
            public void appendTo(StringBuffer buffer) {
                buffer.append("charSequence(\"" + text + "\")");
            }
        });
        return null;
    }

    // Define the set of parameters to run the test with
    // Each set represents a wire type and the expected outcome to validate against
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {WireType.TEXT,
                        (Consumer<WireIn>) w -> assertEquals("handle: !CAPTData {\n" +
                                        "  value: 0\n" +
                                        "}\n" +
                                        "...\n",
                                w.toString())},
                {WireType.YAML_ONLY,
                        (Consumer<WireIn>) w -> assertEquals("handle: !CAPTData {\n" +
                                        "  value: 0\n" +
                                        "}\n" +
                                        "...\n",
                                w.toString())},
                {WireType.BINARY,
                        (Consumer<WireIn>) w -> assertEquals("1f 00 00 00                                     # msg-length\n" +
                                        "b9 06 68 61 6e 64 6c 65                         # handle: (event)\n" +
                                        "b6 08 43 41 50 54 44 61 74 61                   # CAPTData\n" +
                                        "82 08 00 00 00                                  # CAPTData\n" +
                                        "   c5 76 61 6c 75 65                               # value:\n" +
                                        "   a1 00                                           # 0\n",
                                w.bytes().toHexString())},
        });
    }

    // This test verifies the use of custom class lookups in the wire
    @SuppressWarnings({"rawtypes", "unchecked"})
    @ParameterizedTest
    @MethodSource("data")
    public void testUsesClassLookup(WireType wireType, Consumer<Wire> wireChecker) {
        // Create a mock for the ClassLookup interface
        final ClassLookup mock = createMock(ClassLookup.class);

        // Setup expectations for the mock to return the name "CAPTData" when CAPTData.class is provided and vice-versa
        expect(mock.nameFor(CAPTData.class)).andReturn("CAPTData");
        expect(mock.forName(charSequence("CAPTData"))).andReturn((Class) CAPTData.class);

        // Switch the mock to replay mode
        replay(mock);

        // Create a wire with the provided wireType and associate it with a HexDumpBytes
        Wire wire = wireType.apply(new HexDumpBytes());

        // Assign the created mock as the class lookup for the wire
        wire.classLookup(mock);

        // Create a method writer for the TestedMethods interface and write a handle event
        final TestedMethods writer = wire.methodWriter(TestedMethods.class);
        writer.handle(new CAPTData());

        // Validate the content of the wire using the wire checker
        wireChecker.accept(wire);

        // Prepare a StringWriter to capture the output of the method reader
        StringWriter out = new StringWriter();

        // Create a method reader and ensure it's not a proxy class
        final MethodReader reader = wire.methodReader(
                Mocker.logging(TestedMethods.class, "", out));
        String name = reader.getClass().getName();
        assertFalse(name.contains("$Proxy"), name);

        // Read events from the wire and validate their output
        assertTrue(reader.readOne()); // Expect one event to be read
        assertFalse(reader.readOne()); // No more events expected
        assertEquals("" +
                        "handle[!net.openhft.chronicle.wire.ClassAliasPoolTest$CAPTData {\n" +
                        "  value: 0\n" +
                        "}\n" +
                        "]\n",
                out.toString().replace("\r", ""));

        // Verify that the mock was used as expected
        verify(mock);
    }

    // Interface to represent tested methods
    public interface TestedMethods {
        void handle(Marshallable m);
    }

    // Test data class representing a type of event with a single value
    static class CAPTData extends SelfDescribingMarshallable {
        long value;
    }
}
