package net.openhft.chronicle.wire.internal;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.wire.WireType;
import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

// Testing the generation of class names for MethodWriter
public class MethodWriterClassNameGeneratorTest extends net.openhft.chronicle.wire.WireTestCommon {

    // Instance of the class name generator to be tested
    private final MethodWriterClassNameGenerator classNameGenerator = new MethodWriterClassNameGenerator();

    // Test the basic class name generation for a simple interface using YAML WireType
    @Test
    public void testGetClassName() {
        assertEquals("MethodWriterClassNameGeneratorTestJustAnInterfaceYamlMethodWriter",
                generatedClassName(null, false, false, WireType.YAML, JustAnInterface.class));
    }

    // Test the class name generation when metadata is included using JSON WireType
    @Test
    public void testGetClassNameIncludesMetadata() {
        assertEquals("MethodWriterClassNameGeneratorTestJustAnInterfaceMetadataAwareJsonMethodWriter",
                generatedClassName(null, true, false, WireType.JSON, JustAnInterface.class));
    }

    // Test the class name generation when intercepting is included using BINARY_LIGHT WireType
    @Test
    public void testGetClassNameIncludesIntercepting() {
        assertEquals("MethodWriterClassNameGeneratorTestJustAnInterfaceInterceptingBinarylightMethodWriter",
                generatedClassName(null, false, true, WireType.BINARY_LIGHT, JustAnInterface.class));
    }

    // Test the class name generation for a specific generic event using BINARY WireType
    @Test
    public void testGetClassNameIncludesGenericEvent() {
        assertEquals("MethodWriterClassNameGeneratorTestJustAnInterfaceFooBarBinaryMethodWriter",
                generatedClassName("FooBar", false, false, WireType.BINARY, JustAnInterface.class));
    }

    // Test the class name generation when all modifiers are included using RAW WireType
    @Test
    public void testGetClassNameIncludesAllModifiers() {
        assertEquals("MethodWriterClassNameGeneratorTestJustAnInterfaceFooBarMetadataAwareInterceptingRawMethodWriter",
                generatedClassName("FooBar", true, true, WireType.RAW, JustAnInterface.class));
    }

    // Test the class name generation when maximum filename length is exceeded
    @Test
    public void testGetClassNameTruncatesInterfaceNamesWhenMaxFilenameLengthIsExceeded() {
        assertEquals("MethodWriterClassNameGeneratorTestNewOrderSingleListenerOmsHedgerTradeListenerOpenOrdersListenerPaidGivenTickListener1MethodWriterClassNameGeneratorTestNewOrderSingleListenerOmsHedgerTradeListenerOpenOrdersListenerPaidGi5JWELPHK3VKNITextMethodWriter",
                generatedClassName(null, false, false, WireType.TEXT,
                        NewOrderSingleListenerOmsHedgerTradeListenerOpenOrdersListenerPaidGivenTickListener1.class,
                        NewOrderSingleListenerOmsHedgerTradeListenerOpenOrdersListenerPaidGivenTickListener2.class));
    }

    // Test the class name generation with all modifiers and truncation
    @Test
    public void testGetClassNameIncludesAllModifiersTruncated() {
        assertEquals("MethodWriterClassNameGeneratorTestNewOrderSingleListenerOmsHedgerTradeListenerOpenOrdersListenerPaidGivenTickListener1MethodWriterClassNameGeneratorTestNewOrderSingleListenerOmsHCTPGPS5OJWMSQFooBarMetadataAwareInterceptingFieldlessbinaryMethodWriter",
                generatedClassName("FooBar", true, true, WireType.FIELDLESS_BINARY,
                        NewOrderSingleListenerOmsHedgerTradeListenerOpenOrdersListenerPaidGivenTickListener1.class,
                        NewOrderSingleListenerOmsHedgerTradeListenerOpenOrdersListenerPaidGivenTickListener2.class));
    }

    // Test that class names generated with truncation differ when only the truncated portion is different
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

    // Helper method to generate the class name based on provided parameters and interfaces
    private String generatedClassName(String genericEvent, boolean metaData, boolean intercepting, WireType wireType, Class<?>... interfaces) {
        Set<Class<?>> setOfClasses = new LinkedHashSet<>(Arrays.asList(interfaces));
        return classNameGenerator.getClassName(setOfClasses, genericEvent, metaData, intercepting, wireType, false);
    }

    // Simple test interface
    interface JustAnInterface {

    }

    // Complex test interfaces to simulate long names and truncation scenarios
    interface NewOrderSingleListenerOmsHedgerTradeListenerOpenOrdersListenerPaidGivenTickListener1 {
    }

    interface NewOrderSingleListenerOmsHedgerTradeListenerOpenOrdersListenerPaidGivenTickListener2 {
    }

    interface NewOrderSingleListenerOmsHedgerTradeListenerOpenOrdersListenerPaidGi_DiffersInTheTruncatedPortion {
    }
}
