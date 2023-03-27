package net.openhft.chronicle.wire.internal;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.WireType;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class MethodWriterClassNameGeneratorTest {

    private final MethodWriterClassNameGenerator classNameGenerator = new MethodWriterClassNameGenerator();

    @Test
    public void testGetClassName() {
        assertEquals("MethodWriterClassNameGeneratorTestJustAnInterfaceYamlMethodWriter",
                generatedClassName(null, false, false, WireType.YAML, JustAnInterface.class));
    }

    @Test
    public void testGetClassNameIncludesMetadata() {
        assertEquals("MethodWriterClassNameGeneratorTestJustAnInterfaceMetadataAwareJsonMethodWriter",
                generatedClassName(null, true, false, WireType.JSON, JustAnInterface.class));
    }

    @Test
    public void testGetClassNameIncludesIntercepting() {
        assertEquals("MethodWriterClassNameGeneratorTestJustAnInterfaceInterceptingBinarylightMethodWriter",
                generatedClassName(null, false, true, WireType.BINARY_LIGHT, JustAnInterface.class));
    }

    @Test
    public void testGetClassNameIncludesGenericEvent() {
        assertEquals("MethodWriterClassNameGeneratorTestJustAnInterfaceFooBarBinaryMethodWriter",
                generatedClassName("FooBar", false, false, WireType.BINARY, JustAnInterface.class));
    }

    @Test
    public void testGetClassNameIncludesAllModifiers() {
        assertEquals("MethodWriterClassNameGeneratorTestJustAnInterfaceFooBarMetadataAwareInterceptingRawMethodWriter",
                generatedClassName("FooBar", true, true, WireType.RAW, JustAnInterface.class));
    }

    @Test
    public void testGetClassNameTruncatesInterfaceNamesWhenMaxFilenameLengthIsExceeded() {
        assertEquals("MethodWriterClassNameGeneratorTestNewOrderSingleListenerOmsHedgerTradeListenerOpenOrdersListenerPaidGivenTickListener1MethodWriterClassNameGeneratorTestNewOrderSingleListenerOmsHedgerTradeListenerOpenOrdersListenerPaidGi5JWELPHK3VKNITextMethodWriter",
                generatedClassName(null, false, false, WireType.TEXT,
                        NewOrderSingleListenerOmsHedgerTradeListenerOpenOrdersListenerPaidGivenTickListener1.class,
                        NewOrderSingleListenerOmsHedgerTradeListenerOpenOrdersListenerPaidGivenTickListener2.class));
    }

    @Test
    public void testGetClassNameIncludesAllModifiersTruncated() {
        assertEquals("MethodWriterClassNameGeneratorTestNewOrderSingleListenerOmsHedgerTradeListenerOpenOrdersListenerPaidGivenTickListener1MethodWriterClassNameGeneratorTestNewOrderSingleListenerOmsHCTPGPS5OJWMSQFooBarMetadataAwareInterceptingFieldlessbinaryMethodWriter",
                generatedClassName("FooBar", true, true, WireType.FIELDLESS_BINARY,
                        NewOrderSingleListenerOmsHedgerTradeListenerOpenOrdersListenerPaidGivenTickListener1.class,
                        NewOrderSingleListenerOmsHedgerTradeListenerOpenOrdersListenerPaidGivenTickListener2.class));
    }

    @Test
    public void testTruncatedClassNamesDifferWhenOnlyTruncatedPortionDiffers() {
        String cn1 = generatedClassName(null, false, false, WireType.TEXT,
                NewOrderSingleListenerOmsHedgerTradeListenerOpenOrdersListenerPaidGivenTickListener1.class,
                NewOrderSingleListenerOmsHedgerTradeListenerOpenOrdersListenerPaidGivenTickListener2.class);
        String cn2 = generatedClassName(null, false, false, WireType.TEXT,
                NewOrderSingleListenerOmsHedgerTradeListenerOpenOrdersListenerPaidGivenTickListener1.class,
                NewOrderSingleListenerOmsHedgerTradeListenerOpenOrdersListenerPaidGi_DiffersInTheTruncatedPortion.class);
        Jvm.startup().on(MethodWriterClassNameGeneratorTest.class, "Must differ:\n" + cn1 + "\n" + cn2);
        assertNotEquals(cn1, cn2);
    }

    private String generatedClassName(String genericEvent, boolean metaData, boolean intercepting, WireType wireType, Class<?>... interfaces) {
        Set<Class<?>> setOfClasses = new LinkedHashSet<>(Arrays.asList(interfaces));
        return classNameGenerator.getClassName(setOfClasses, genericEvent, metaData, intercepting, wireType);
    }

    interface JustAnInterface {

    }

    interface NewOrderSingleListenerOmsHedgerTradeListenerOpenOrdersListenerPaidGivenTickListener1 {
    }

    interface NewOrderSingleListenerOmsHedgerTradeListenerOpenOrdersListenerPaidGivenTickListener2 {
    }

    interface NewOrderSingleListenerOmsHedgerTradeListenerOpenOrdersListenerPaidGi_DiffersInTheTruncatedPortion {
    }
}