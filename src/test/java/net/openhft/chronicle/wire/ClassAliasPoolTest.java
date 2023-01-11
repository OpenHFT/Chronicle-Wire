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
import net.openhft.chronicle.core.Mocker;
import net.openhft.chronicle.core.pool.ClassLookup;
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
    private final WireType wireType;
    private final Consumer<Wire> wireChecker;

    public ClassAliasPoolTest(WireType wireType, Consumer<Wire> wireChecker) {
        this.wireType = wireType;
        this.wireChecker = wireChecker;
    }

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

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {WireType.TEXT,
                        (Consumer<WireIn>) w -> assertEquals("" +
                                        "handle: !CAPTData {\n" +
                                        "  value: 0\n" +
                                        "}\n" +
                                        "...\n",
                                w.toString())},
                {WireType.BINARY,
                        (Consumer<WireIn>) w -> assertEquals("" +
                                        "1f 00 00 00                                     # msg-length\n" +
                                        "b9 06 68 61 6e 64 6c 65                         # handle: (event)\n" +
                                        "b6 08 43 41 50 54 44 61 74 61                   # CAPTData\n" +
                                        "82 08 00 00 00                                  # CAPTData\n" +
                                        "   c5 76 61 6c 75 65                               # value:\n" +
                                        "   a1 00                                           # 0\n",
                                w.bytes().toHexString())},
        });
    }

    @Test
    public void testUsesClassLookup() {
        final ClassLookup mock = createMock(ClassLookup.class);
        expect(mock.nameFor(CAPTData.class)).andReturn("CAPTData");
        expect(mock.forName(charSequence("CAPTData"))).andReturn((Class) CAPTData.class);
        replay(mock);

        Wire wire = wireType.apply(new HexDumpBytes());
        wire.classLookup(mock);
        final TestedMethods writer = wire.methodWriter(TestedMethods.class);
        writer.handle(new CAPTData());

        wireChecker.accept(wire);
        StringWriter out = new StringWriter();
        final MethodReader reader = wire.methodReader(
                Mocker.logging(TestedMethods.class, "", out));
        assertTrue(reader.readOne());
        assertFalse(reader.readOne());
        assertEquals("" +
                        "handle[!net.openhft.chronicle.wire.ClassAliasPoolTest$CAPTData {\n" +
                        "  value: 0\n" +
                        "}\n" +
                        "]\n",
                out.toString().replace("\r", ""));
        verify(mock);
    }

    public interface TestedMethods {
        void handle(Marshallable m);
    }

    public static class CAPTData extends SelfDescribingMarshallable {
        long value;
    }
}
