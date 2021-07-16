package net.openhft.chronicle.wire;

import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.core.StringContains.containsString;

// see https://github.com/OpenHFT/Chronicle-Wire/issues/240
public class SmallDoublesMarshallingTest extends WireTestCommon {
    public static class Example extends SelfDescribingMarshallable {
        private double doubleVal;

        public double doubleVal() {
            return doubleVal;
        }

        public Example doubleVal(double doubleVal) {
            this.doubleVal = doubleVal;
            return this;
        }
    }

    @Test
    public void marshallingTest() {
        final Example example = new Example().doubleVal(1.104326320059551E-14);
        final String textRepr = example.toString();

        final Example demarshalled = WireType.TEXT.fromString(Example.class, textRepr);

        MatcherAssert.assertThat(textRepr, containsString("1.104326320059551E-14"));
        Assert.assertEquals(example.doubleVal(), demarshalled.doubleVal(), 1e-14);
    }
}
