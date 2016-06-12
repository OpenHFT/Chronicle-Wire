package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.annotation.UsedViaReflection;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static net.openhft.chronicle.core.pool.ClassAliasPool.CLASS_ALIASES;

/**
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
                {WireType.TEXT},
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

        try (DocumentContext dc = wire.readingDocument()) {
            if (!dc.isPresent())
                Assert.fail();
            DTO2 dto2 = dc.wire().getValueIn().typedMarshallable();
            Assert.assertEquals(dto2.one, 1);
            Assert.assertEquals(dto2.two, 0);
            Assert.assertEquals(dto2.three, null);
        }

    }

    @Test
    public void forwardComparability() throws Exception {

        final Wire wire = wireType.apply(Bytes.elasticByteBuffer());
        CLASS_ALIASES.addAlias(DTO2.class, "DTO");

        wire.writeDocument(false, w -> w.getValueOut().typedMarshallable(new DTO2(1, 2, 3)));
        System.out.println(Wires.fromSizePrefixedBlobs(wire));

        CLASS_ALIASES.addAlias(DTO1.class, "DTO");

        try (DocumentContext dc = wire.readingDocument()) {
            if (!dc.isPresent())
                Assert.fail();
            DTO1 dto1 = dc.wire().getValueIn().typedMarshallable();
            Assert.assertEquals(dto1.one, 1);
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

        public DTO2 three(Object three) {
            this.three = three;
            return this;
        }

        public int one() {
            return one;
        }

        public DTO2 one(int one) {
            this.one = one;
            return this;
        }

        public int two() {
            return two;
        }

        public DTO2 two(int two) {
            this.two = two;
            return this;
        }
    }
}

