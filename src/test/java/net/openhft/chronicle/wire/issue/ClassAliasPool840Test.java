/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.issue;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.core.util.ClassNotFoundRuntimeException;
import net.openhft.chronicle.wire.SelfDescribingMarshallable;
import net.openhft.chronicle.wire.ValueIn;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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

        Dto value(long value) {
            this.value = value;
            return this;
        }
    }

    private static class Type extends SelfDescribingMarshallable {
    }
}
