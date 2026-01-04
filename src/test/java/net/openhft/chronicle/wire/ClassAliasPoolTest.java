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
import org.junit.jupiter.api.DisplayName;
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
                buffer.append("charSequence(\"").append(text).append("\\')");
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
                                w.toString(),
                                "text wire output should include CAPTData handle event")},
                {WireType.YAML_ONLY,
                        (Consumer<WireIn>) w -> assertEquals("handle: !CAPTData {\n" +
                                        "  value: 0\n" +
                                        "}\n" +
                                        "...\n",
                                w.toString(),
                                "yaml wire output should include CAPTData handle event")},
                {WireType.BINARY,
                        (Consumer<WireIn>) w -> assertEquals("1f 00 00 00                                     # msg-length\n" +
                                        "b9 06 68 61 6e 64 6c 65                         # handle: (event)\n" +
                                        "b6 08 43 41 50 54 44 61 74 61                   # CAPTData\n" +
                                        "82 08 00 00 00                                  # CAPTData\n" +
                                        "   c5 76 61 6c 75 65                               # value:\n" +
                                        "   a1 00                                           # 0\n",
                                w.bytes().toHexString(),
                                "binary wire hex output should match CAPTData handle event")},
        });
    }

    // This test verifies the use of custom class lookups in the wire
    @MethodSource("data")
    @SuppressWarnings({"rawtypes", "unchecked"})
    @ParameterizedTest(name = "{0}")
    @DisplayName("Uses class lookup aliases while reading and writing")
    void testUsesClassLookup(WireType wireType, Consumer<Wire> wireChecker) {
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
        CAPTData data = new CAPTData();
        assertEquals(0L, data.value, "Expected CAPTData default value to be zero");
        writer.handle(data);

        // Validate the content of the wire using the wire checker
        wireChecker.accept(wire);

        // Prepare a StringWriter to capture the output of the method reader
        StringWriter out = new StringWriter();

        // Create a method reader and ensure it's not a proxy class
        final MethodReader reader = wire.methodReader(
                Mocker.logging(TestedMethods.class, "", out));
        String name = reader.getClass().getName();
        assertFalse(name.contains("$Proxy"),
                "method reader should not be a proxy class for wireType=" + wireType + ", className=" + name);

        // Read events from the wire and validate their output
        assertTrue(reader.readOne(),
                "method reader should read one event for wireType=" + wireType);
        assertFalse(reader.readOne(),
                "method reader should have no extra events for wireType=" + wireType);
        assertEquals("handle[!net.openhft.chronicle.wire.ClassAliasPoolTest$CAPTData {\n" +
                        "  value: 0\n" +
                        "}\n" +
                        "]\n",
                out.toString().replace("\r", ""),
                "method reader output should include CAPTData handle for wireType=" + wireType);

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
