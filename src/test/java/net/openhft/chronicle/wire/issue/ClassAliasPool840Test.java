/*
 * Copyright 2016-2025 chronicle.software
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
package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.core.util.ClassNotFoundRuntimeException;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.ValueIn;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ClassAliasPool840Test {
    /**
     * <a href="https://github.com/OpenHFT/Chronicle-Wire/issues/840">Chronicle-Wire#840</a>
     */
    @Test
    public void typeIsLoadedByClassLookup() {
        ClassLookup customClassLookup = new ClassLookup() {
            @Override
            public Class<?> forName(CharSequence name) throws ClassNotFoundRuntimeException {
                switch (name.toString()) {
                    case "Dto": return Dto.class;
                    case "Type"   : return Type.class;
                    case "type"   : return Class.class;
                    default: throw new IllegalStateException();
                }
            }

            @Override
            public String nameFor(Class<?> clazz) throws IllegalArgumentException {
                if (clazz.equals(Dto.class)) return "Dto";
                if (clazz.equals(Type.class))    return "Type";
                if (clazz.equals(Class.class))    return "type";

                throw new IllegalStateException();
            }

            @Override
            public void addAlias(Class<?>... classes) {}

            @Override
            public void addAlias(Class<?> clazz, String names) {}
        };

        Wire wire = WireType.YAML_ONLY.apply(Bytes.allocateElasticOnHeap());
        wire.classLookup(customClassLookup);

        wire.reset();
        wire.bytes().clear().append(
            "{ " +
            "obj: !Dto { value: 1 }, " +
            "clazz: !type Type " +
            "}");

        StringBuilder name = new StringBuilder();
        while (wire.hasMore()) {
            ValueIn in = wire.read(name);
            Object o = in.typedMarshallable();

            if ("obj".contentEquals(name)) {
                assertEquals(new Dto().value(1), o);
            } else {
                assertEquals(Type.class, o);
            }
        }
    }

    public static class Dto extends SelfDescribingMarshallable {
        private long value;

        public long value() {
            return value;
        }

        public Dto value(long value) {
            this.value = value;
            return this;
        }
    }

    public static class Type extends SelfDescribingMarshallable {
    }
}
