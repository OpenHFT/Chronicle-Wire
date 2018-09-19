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
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(value = Parameterized.class)
public class Marshallable2Test {
    private final WireType wireType;

    public Marshallable2Test(WireType wireType) {
        this.wireType = wireType;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> wireTypes() {
        return Arrays.asList(
                new Object[]{WireType.BINARY},
                new Object[]{WireType.TEXT}
        );
    }

    @Test
    public void testObject() {
        Bytes bytes = Bytes.elasticHeapByteBuffer(64);
        Wire wire = wireType.apply(bytes);

        Outer source = new Outer("Armadillo");
        source.inner2 = new Inner2();

        wire.getValueOut().object(source);
        Outer target = wire.getValueIn().object(source.getClass());
        Assert.assertEquals(source, target);
    }

    private static class Outer extends AbstractMarshallable {
        String name;
        Inner1 inner1;
        Inner2 inner2;

        public Outer(String name) {
            this.name = name;
        }
    }

    private static class Inner1 extends AbstractMarshallable {
    }

    private static class Inner2 extends AbstractMarshallable {
    }
}
