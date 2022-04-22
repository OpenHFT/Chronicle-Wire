/*
 * Copyright 2016-2020 chronicle.software
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
import net.openhft.chronicle.core.annotation.UsedViaReflection;
import net.openhft.chronicle.core.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static net.openhft.chronicle.core.pool.ClassAliasPool.CLASS_ALIASES;

@RunWith(value = Parameterized.class)
public class ForwardAndBackwardCompatibilityMarshallableTest extends WireTestCommon {

    private final WireType wireType;

    public ForwardAndBackwardCompatibilityMarshallableTest(WireType wireType) {
        this.wireType = wireType;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {WireType.JSON},
                {WireType.TEXT},
                {WireType.BINARY}
        });
    }

    @Test
    public void marshableStringBuilderTest() throws Exception {
        final Wire wire = wireType.apply(Bytes.elasticByteBuffer());
        wire.usePadding(true);
        CLASS_ALIASES.addAlias(MDTO2.class, "MDTO");

        wire.writeDocument(false, w -> new MDTO2(1, 2, "3").writeMarshallable(w));
       // System.out.println(Wires.fromSizePrefixedBlobs(wire));

        if (wire instanceof TextWire)
            ((TextWire) wire).useBinaryDocuments();

        try (DocumentContext dc = wire.readingDocument()) {
            if (!dc.isPresent())
                Assert.fail();
            @NotNull MDTO2 dto2 = new MDTO2();
            dto2.readMarshallable(dc.wire());
            Assert.assertEquals(1, dto2.one );
            Assert.assertEquals(2, dto2.two);
            Assert.assertTrue("3".contentEquals(dto2.three));
        }

        wire.bytes().releaseLast();
    }

    @Test
    public void backwardsCompatibility() {
        expectException("Replaced class net.openhft.chronicle.wire.ForwardAndBackwardCompatibilityMarshallableTest$MDTO1 with class net.openhft.chronicle.wire.ForwardAndBackwardCompatibilityMarshallableTest$MDTO2");

        final Wire wire = wireType.apply(Bytes.elasticByteBuffer());
        wire.usePadding(true);
        CLASS_ALIASES.addAlias(MDTO1.class, "MDTO");

        wire.writeDocument(false, w -> w.getValueOut().typedMarshallable(new MDTO1(1)));
       // System.out.println(Wires.fromSizePrefixedBlobs(wire));

        CLASS_ALIASES.addAlias(MDTO2.class, "MDTO");
        if (wire instanceof TextWire)
            ((TextWire) wire).useBinaryDocuments();
        try (DocumentContext dc = wire.readingDocument()) {
            if (!dc.isPresent())
                Assert.fail();
            @NotNull MDTO2 dto2 = new MDTO2();
            dc.wire().getValueIn().marshallable(dto2);
            Assert.assertEquals(1, dto2.one);
            Assert.assertEquals(0, dto2.two);

        }

        wire.bytes().releaseLast();
    }

    @Test
    public void forwardCompatibility() {
        expectException("Replaced class net.openhft.chronicle.wire.ForwardAndBackwardCompatibilityMarshallableTest$MDTO2 with class net.openhft.chronicle.wire.ForwardAndBackwardCompatibilityMarshallableTest$MDTO1");

        final Wire wire = wireType.apply(Bytes.elasticByteBuffer());
        wire.usePadding(true);
        CLASS_ALIASES.addAlias(MDTO2.class, "MDTO");

        wire.writeDocument(false, w -> w.getValueOut().typedMarshallable(new MDTO2(1, 2, "3")));
       // System.out.println(Wires.fromSizePrefixedBlobs(wire));

        CLASS_ALIASES.addAlias(MDTO1.class, "MDTO");

        if (wire instanceof TextWire)
            ((TextWire) wire).useBinaryDocuments();
        try (DocumentContext dc = wire.readingDocument()) {
            if (!dc.isPresent())
                Assert.fail();
            @NotNull MDTO1 dto1 = new MDTO1();
            dc.wire().getValueIn()
                    .marshallable(dto1);
            Assert.assertEquals(1, dto1.one);
        }

        wire.bytes().releaseLast();
    }

    public static class MDTO1 extends SelfDescribingMarshallable implements Demarshallable {

        int one;

        @UsedViaReflection
        public MDTO1(@NotNull WireIn wire) {
            readMarshallable(wire);
        }

        public MDTO1(int i) {
            this.one = i;
        }

        public MDTO1() {

        }

        public int one() {
            return one;
        }

        @NotNull
        public MDTO1 one(int one) {
            this.one = one;
            return this;
        }
    }

    public static class MDTO2 extends SelfDescribingMarshallable implements Demarshallable {

        StringBuilder three = new StringBuilder();
        int one;
        int two;

        @UsedViaReflection
        public MDTO2(@NotNull WireIn wire) {
            readMarshallable(wire);
        }

        public MDTO2(int one, int two, @NotNull Object three) {
            this.one = one;
            this.two = two;
            StringUtils.setCount(this.three, 0);
            this.three.append(three.toString());
        }

        public MDTO2() {

        }

        @NotNull
        public Object three() {
            return three;
        }

        @NotNull
        public MDTO2 three(@NotNull Object three) {
            StringUtils.setCount(this.three, 0);
            this.three.append(three.toString());
            return this;
        }

        public int one() {
            return one;
        }

        @NotNull
        public MDTO2 one(int one) {
            this.one = one;
            return this;
        }

        public int two() {
            return two;
        }

        @NotNull
        public MDTO2 two(int two) {
            this.two = two;
            return this;
        }
    }
}

