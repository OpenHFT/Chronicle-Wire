/*
 * Copyright 2016 higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire.marshallable;

import net.openhft.chronicle.wire.AbstractMarshallable;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/*
 * Created by Peter Lawrey on 18/10/16.
 */
public class CompareNaNTest {
    @Test
    public void testPrim() {
        @NotNull PrimDTO a = new PrimDTO(Double.NaN, Float.NaN);
        @NotNull PrimDTO b = new PrimDTO(Double.NaN, Float.NaN);
        assertEquals(a.toString(), b.toString());
        assertEquals(a, b);
    }

    @Test
    public void testWrapDTO() {
        @NotNull WrapDTO a = new WrapDTO(Double.NaN, Float.NaN);
        @NotNull WrapDTO b = new WrapDTO(Double.NaN, Float.NaN);
        assertEquals(a.toString(), b.toString());
        assertEquals(a, b);
    }

    @Test
    public void testObjectWrapDTO() {
        @NotNull ObjectWrapDTO a = new ObjectWrapDTO(Double.NaN, Float.NaN);
        @NotNull ObjectWrapDTO b = new ObjectWrapDTO(Double.NaN, Float.NaN);
        assertEquals(a.toString(), b.toString());
        assertEquals(a, b);
    }

    static class PrimDTO extends AbstractMarshallable {
        double d;
        float f;

        public PrimDTO(double d, float f) {
            this.d = d;
            this.f = f;
        }
    }

    static class WrapDTO extends AbstractMarshallable {
        Double d;
        Float f;

        public WrapDTO(Double d, Float f) {
            this.d = d;
            this.f = f;
        }
    }

    static class ObjectWrapDTO extends AbstractMarshallable {
        Object d;
        Object f;

        public ObjectWrapDTO(Object d, Object f) {
            this.d = d;
            this.f = f;
        }
    }
}
