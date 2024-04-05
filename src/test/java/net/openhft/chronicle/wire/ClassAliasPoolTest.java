/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.core.util.Mocker;
import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class ClassAliasPoolTest extends WireTestCommon {

    // Define the type of wire (e.g., TEXT, YAML, BINARY) being tested
    private final WireType wireType;

    // Define a consumer that performs checks on the wire's content
    private final Consumer<Wire> wireChecker;

    // Constructor to initialize wire type and checker
    public ClassAliasPoolTest(WireType wireType, Consumer<Wire> wireChecker) {
        this.wireType = wireType;
        this.wireChecker = wireChecker;
    }

    // Helper method to match char sequences in a mock setup
    public static CharSequence charSequence(String text) {
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
    @Parameterized.Parameters(name = "{0}")
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
    @Test
    public void testUsesClassLookup() {
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
        assertFalse(name, name.contains("$Proxy"));

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
    public static class CAPTData extends SelfDescribingMarshallable {
        long value;
    }
}
