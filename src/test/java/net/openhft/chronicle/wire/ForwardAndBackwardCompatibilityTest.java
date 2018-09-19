/*
 * Copyright 2016 higherfrequencytrading.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.core.annotation.UsedViaReflection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static net.openhft.chronicle.core.pool.ClassAliasPool.CLASS_ALIASES;

/*
 * Created by rob on 03/05/2016.
 */

@RunWith(value = Parameterized.class)
public class ForwardAndBackwardCompatibilityTest {

    private final WireType wireType;

    public ForwardAndBackwardCompatibilityTest(WireType wireType) {
        this.wireType = wireType;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
//                {WireType.TEXT},
                {WireType.BINARY}

        });
    }

    @Test
    public void backwardsCompatibility() throws Exception {
        final Wire wire = wireType.apply(Bytes.elasticByteBuffer());
        CLASS_ALIASES.addAlias(DTO1.class, "DTO");

        wire.writeDocument(false, w -> w.getValueOut().typedMarshallable(new DTO1(1)));
        System.out.println(Wires.fromSizePrefixedBlobs(wire));

        CLASS_ALIASES.addAlias(DTO2.class, "DTO");
        if (wire instanceof TextWire)
            ((TextWire) wire).useBinaryDocuments();

        try (DocumentContext dc = wire.readingDocument()) {
            if (!dc.isPresent())
                Assert.fail();
            @Nullable DTO2 dto2 = dc.wire().getValueIn().typedMarshallable();
            Assert.assertEquals(dto2.one, 1);
            Assert.assertEquals(dto2.two, 0);
            Assert.assertEquals(dto2.three, null);
        }

        wire.bytes().release();
    }

    @Test
    public void forwardComparability() {

        final Wire wire = wireType.apply(Bytes.elasticByteBuffer());
        CLASS_ALIASES.addAlias(DTO2.class, "DTO");

        wire.writeDocument(false, w -> w.getValueOut().typedMarshallable(new DTO2(1, 2, 3)));
        System.out.println(Wires.fromSizePrefixedBlobs(wire));

        CLASS_ALIASES.addAlias(DTO1.class, "DTO");
        if (wire instanceof TextWire)
            ((TextWire) wire).useBinaryDocuments();

        try (DocumentContext dc = wire.readingDocument()) {
            if (!dc.isPresent())
                Assert.fail();
            @Nullable DTO1 dto1 = dc.wire().getValueIn().typedMarshallable();
            Assert.assertEquals(dto1.one, 1);
        }

        wire.bytes().release();
    }

    @After
    public void checkRegisteredBytes() {
        BytesUtil.checkRegisteredBytes();
    }

    @Test
    public void testCheckThatNewDataAddedToADocumentDoesNotEffectOldReads() {

        Bytes b = Bytes.elasticByteBuffer();
        try {
            Wire w = WireType.FIELDLESS_BINARY.apply(b);

            try (DocumentContext dc = w.writingDocument()) {
                dc.wire().write("hello").text("hello world");
                dc.wire().write("hello2").text("hello world");
            }

            try (DocumentContext dc = w.writingDocument()) {
                dc.wire().write("other data").text("other data");
            }

            try (DocumentContext dc = w.readingDocument()) {
                Assert.assertEquals("hello world", dc.wire().read("hello").text());
            }

            try (DocumentContext dc = w.readingDocument()) {
                Assert.assertEquals("other data", dc.wire().read("other data").text());
            }
        } finally {
            b.release();
        }
    }

    public static class DTO1 extends AbstractMarshallable implements Demarshallable {

        int one;

        @UsedViaReflection
        public DTO1(@NotNull WireIn wire) {
            readMarshallable(wire);
        }

        public DTO1(int i) {
            this.one = i;
        }

        public int one() {
            return one;
        }

        @NotNull
        public DTO1 one(int one) {
            this.one = one;
            return this;
        }
    }

    public static class DTO2 extends AbstractMarshallable implements Demarshallable {
        Object three;
        int one;
        int two;
        Object o;

        @UsedViaReflection
        public DTO2(@NotNull WireIn wire) {
            readMarshallable(wire);
        }

        public DTO2(int one, int two, Object three) {
            this.one = one;
            this.two = two;
            this.three = three;
        }

        public Object three() {
            return three;
        }

        @NotNull
        public DTO2 three(Object three) {
            this.three = three;
            return this;
        }

        public int one() {
            return one;
        }

        @NotNull
        public DTO2 one(int one) {
            this.one = one;
            return this;
        }

        public int two() {
            return two;
        }

        @NotNull
        public DTO2 two(int two) {
            this.two = two;
            return this;
        }
    }

}

