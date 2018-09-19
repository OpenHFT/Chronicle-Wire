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

package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/*
 * Created by Peter Lawrey on 11/12/16.
 */
public class OverrideAValueTest {
    @Test
    public void testDontTouchImmutables() {
        @Nullable NumberHolder nh = Marshallable.fromString("!" + NumberHolder.class.getName() + " { num: 2 } ");
        assertEquals(1, NumberHolder.ONE.intValue());
        assertEquals(2, nh.num.intValue());
    }

    @Test
    public void testDontTouchImmutables2() {
        ObjectUtils.immutabile(NumberHolder.class, true);
        @Nullable ObjectHolder oh = Marshallable.fromString("!" + ObjectHolder.class.getName() + " { nh: !" + NumberHolder.class.getName() + " { num: 3 } } ");
        assertEquals(1, NumberHolder.ONE.intValue());
        assertEquals(1, ObjectHolder.NH.num.intValue());
        assertEquals(3, oh.nh.num.intValue());
    }

    @Test
    public void testAllowClassChange() {
        @Nullable ParentHolder ph = Marshallable.fromString("!" + ParentHolder.class.getName() + " { object: !" + SubClass.class.getName() + " { name: bob, value: 3.3 } } ");
        assertEquals("!net.openhft.chronicle.wire.OverrideAValueTest$ParentHolder {\n" +
                "  object: !net.openhft.chronicle.wire.OverrideAValueTest$SubClass {\n" +
                "    name: bob,\n" +
                "    value: 3.3\n" +
                "  }\n" +
                "}\n", ph.toString());
    }

    static class NumberHolder extends AbstractMarshallable {
        @SuppressWarnings("UnnecessaryBoxing")
        public static final Integer ONE = new Integer(1);
        @NotNull
        Integer num = ONE;
    }

    static class ObjectHolder extends AbstractMarshallable {
        @SuppressWarnings("UnnecessaryBoxing")
        public static final NumberHolder NH = new NumberHolder();
        @NotNull
        NumberHolder nh = NH;
    }

    static class ParentClass extends AbstractMarshallable {
        @NotNull
        String name = "name";
    }

    static class SubClass extends ParentClass {
        double value = 1.28;
    }

    static class ParentHolder extends AbstractMarshallable {
        final ParentClass object = new ParentClass();
    }
}
