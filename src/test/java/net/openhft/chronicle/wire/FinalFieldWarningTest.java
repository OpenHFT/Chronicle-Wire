/*
 * Copyright 2013-2026 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.onoes.ExceptionKey;
import net.openhft.chronicle.core.onoes.LogLevel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static net.openhft.chronicle.wire.WireMarshaller.WIRE_MARSHALLER_CL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class FinalFieldWarningTest {

    @Before
    public void useWarningModeByDefault() {
        WireMarshaller.strictFinalFields(false);
    }

    @After
    public void reset() {
        WireMarshaller.strictFinalFields(false);
        Jvm.resetExceptionHandlers();
    }

    @Test
    public void serialisingFinalFieldDoesNotWarn() {
        final Map<ExceptionKey, Integer> exceptions = Jvm.recordExceptions();
        final Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        try {
            new WriteOnlyFinal(7).writeMarshallable(WireType.TEXT.apply(bytes));
        } finally {
            bytes.releaseLast();
            Jvm.resetExceptionHandlers();
        }

        assertEquals(0, warningCount(exceptions, WriteOnlyFinal.class));
    }

    @Test
    public void actualReadWarnsOncePerClass() throws InvalidMarshallableException {
        final Map<ExceptionKey, Integer> exceptions = Jvm.recordExceptions();
        try {
            read(new ReadWarnFinal(1), "id: 2\n", true);
            read(new ReadWarnFinal(3), "id: 4\n", true);
        } finally {
            Jvm.resetExceptionHandlers();
        }

        assertEquals(1, warningCount(exceptions, ReadWarnFinal.class));
    }

    @Test
    public void strictModeIsAppliedAfterMarshallerWasCached() {
        assertNotNull(WIRE_MARSHALLER_CL.get(StrictAfterCache.class));
        final StrictAfterCache target = new StrictAfterCache(1);
        WireMarshaller.strictFinalFields(true);

        final IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> read(target, "id: 9\n", true));

        assertTrue(thrown.getMessage().contains("final field 'id'"));
        assertEquals(1, target.id);
    }

    @Test
    public void missingFinalFieldDoesNotWarnWhenOverwriteIsFalse() throws InvalidMarshallableException {
        final Map<ExceptionKey, Integer> exceptions = Jvm.recordExceptions();
        final MissingFinal target = new MissingFinal(7);
        try {
            read(target, "name: changed\n", false);
        } finally {
            Jvm.resetExceptionHandlers();
        }

        assertEquals(0, warningCount(exceptions, MissingFinal.class));
        assertEquals(7, target.id);
    }

    @Test
    public void missingFinalFieldWarnsWhenOverwriteResetsIt() throws InvalidMarshallableException {
        final Map<ExceptionKey, Integer> exceptions = Jvm.recordExceptions();
        final ResetMissingFinal target = new ResetMissingFinal(7);
        try {
            read(target, "name: changed\n", true);
        } finally {
            Jvm.resetExceptionHandlers();
        }

        assertEquals(1, warningCount(exceptions, ResetMissingFinal.class));
    }

    @Test
    public void mutatingContentsOfFinalCollectionDoesNotFailStrictMode() throws InvalidMarshallableException {
        final FinalCollection target = new FinalCollection();
        WireMarshaller.strictFinalFields(true);

        read(target, "values: [ one, two ]\n", true);

        assertEquals(2, target.values.size());
        assertEquals("one", target.values.get(0));
    }

    @Test
    public void mutatingContentsOfFinalStringBuilderDoesNotFailStrictMode() throws InvalidMarshallableException {
        final FinalStringBuilder target = new FinalStringBuilder();
        WireMarshaller.strictFinalFields(true);

        read(target, "text: changed\n", true);

        assertEquals("changed", target.text.toString());
    }

    @Test
    public void throwableMarshallerUsesTheSameReadPolicy() {
        assertNotNull(WIRE_MARSHALLER_CL.get(FinalFieldException.class));
        WireMarshaller.strictFinalFields(true);

        assertThrows(IllegalStateException.class,
                () -> read(new FinalFieldException(1), "code: 2\n", true));
    }

    @Test
    public void classWithoutFinalFieldsReadsInStrictMode() throws InvalidMarshallableException {
        final NoFinalField target = new NoFinalField();
        WireMarshaller.strictFinalFields(true);

        read(target, "id: 2\n", true);

        assertEquals(2, target.id);
    }

    private static void read(Object target, String text, boolean overwrite) throws InvalidMarshallableException {
        final Bytes<?> bytes = Bytes.from(text);
        try {
            Wires.readMarshallable(target, WireType.TEXT.apply(bytes), overwrite);
        } finally {
            bytes.releaseLast();
        }
    }

    private static int warningCount(Map<ExceptionKey, Integer> exceptions, Class<?> type) {
        return exceptions.entrySet().stream()
                .filter(e -> e.getKey().level == LogLevel.WARN)
                .filter(e -> e.getKey().message != null)
                .filter(e -> e.getKey().message.contains(type.getName()))
                .filter(e -> e.getKey().message.contains("field-based deserialisation"))
                .mapToInt(Map.Entry::getValue)
                .sum();
    }

    static class WriteOnlyFinal extends SelfDescribingMarshallable {
        final int id;

        WriteOnlyFinal(int id) {
            this.id = id;
        }
    }

    static class ReadWarnFinal extends SelfDescribingMarshallable {
        final int id;

        ReadWarnFinal(int id) {
            this.id = id;
        }
    }

    static class StrictAfterCache extends SelfDescribingMarshallable {
        final int id;

        StrictAfterCache(int id) {
            this.id = id;
        }
    }

    static class MissingFinal extends SelfDescribingMarshallable {
        final int id;
        String name;

        MissingFinal(int id) {
            this.id = id;
        }
    }

    static class ResetMissingFinal extends SelfDescribingMarshallable {
        final int id;
        String name;

        ResetMissingFinal() {
            this(0);
        }

        ResetMissingFinal(int id) {
            this.id = id;
        }
    }

    static class FinalCollection extends SelfDescribingMarshallable {
        final List<String> values = new ArrayList<>();
    }

    static class FinalStringBuilder extends SelfDescribingMarshallable {
        final StringBuilder text = new StringBuilder("initial");
    }

    static class FinalFieldException extends RuntimeException {
        private static final long serialVersionUID = 0L;
        final int code;

        FinalFieldException(int code) {
            this.code = code;
        }
    }

    static class NoFinalField extends SelfDescribingMarshallable {
        int id;
    }
}
