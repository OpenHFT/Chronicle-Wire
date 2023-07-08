package net.openhft.chronicle.wire;

import java.io.FilePermission;
import java.lang.management.ManagementPermission;
import java.lang.reflect.ReflectPermission;
import java.lang.reflect.Type;
import java.nio.BufferUnderflowException;
import java.security.Permission;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.PropertyPermission;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ObjDoubleConsumer;
import java.util.function.ObjIntConsumer;
import java.util.function.ObjLongConsumer;
import java.util.function.Supplier;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.PointerBytesStore;
import net.openhft.chronicle.bytes.ReadBytesMarshallable;

import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.pool.ClassLookup;
import net.openhft.chronicle.core.util.BooleanConsumer;
import net.openhft.chronicle.core.util.ObjBooleanConsumer;

import net.openhft.chronicle.core.util.ObjByteConsumer;

import net.openhft.chronicle.core.util.ObjFloatConsumer;
import net.openhft.chronicle.core.util.ObjShortConsumer;
import net.openhft.chronicle.core.values.BooleanValue;
import net.openhft.chronicle.core.values.IntValue;
import net.openhft.chronicle.core.values.LongArrayValues;
import net.openhft.chronicle.core.values.LongValue;
import org.junit.Ignore;
import org.junit.Test;

public class YamlWireBlueTest {
    /**
     * Method under test: {@link YamlWire#YamlWire()}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testConstructor() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access 'sun.misc'.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange and Act
        // TODO: Populate arranged inputs
        YamlWire actualYamlWire = new YamlWire();

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#applyToMarshallable(Function)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInApplyToMarshallable() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access 'sun.misc'.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        Function<WireIn, Object> marshallableReader = null;

        // Act
        Object actualApplyToMarshallableResult = textValueIn.applyToMarshallable(marshallableReader);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#bool()}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInBool() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access 'sun.misc'.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;

        // Act
        boolean actualBoolResult = textValueIn.bool();

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#bool(Object, ObjBooleanConsumer)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInBool2() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access 'sun.misc'.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        Object object = null;
        ObjBooleanConsumer<Object> tFlag = null;

        // Act
        WireIn actualBoolResult = textValueIn.bool(object, tFlag);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#bool(BooleanValue)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInBool3() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access 'sun.misc'.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        BooleanValue value = null;

        // Act
        WireIn actualBoolResult = textValueIn.bool(value);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#bytes(BytesOut)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInBytes() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access 'sun.misc'.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        BytesOut<?> toBytes = null;

        // Act
        WireIn actualBytesResult = textValueIn.bytes(toBytes);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#bytes(ReadBytesMarshallable)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInBytes2() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access 'sun.misc'.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        ReadBytesMarshallable bytesConsumer = null;

        // Act
        WireIn actualBytesResult = textValueIn.bytes(bytesConsumer);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#bytes(byte[])}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInBytes3() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access 'sun.misc'.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        byte[] using = null;

        // Act
        byte[] actualBytesResult = textValueIn.bytes(using);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#bytesMatch(BytesStore, BooleanConsumer)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInBytesMatch() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access 'sun.misc'.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        BytesStore compareBytes = null;
        BooleanConsumer consumer = null;

        // Act
        WireIn actualBytesMatchResult = textValueIn.bytesMatch(compareBytes, consumer);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#bytesSet(PointerBytesStore)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInBytesSet() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access 'sun.misc'.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        PointerBytesStore toBytes = null;

        // Act
        WireIn actualBytesSetResult = textValueIn.bytesSet(toBytes);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#classLookup()}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInClassLookup() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access 'sun.misc'.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;

        // Act
        ClassLookup actualClassLookupResult = textValueIn.classLookup();

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#consumeAny(int)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInConsumeAny() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access 'sun.misc'.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        int minIndent = 0;

        // Act
        textValueIn.consumeAny(minIndent);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#date(Object, BiConsumer)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInDate() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access 'sun.misc'.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        Object object = null;
        BiConsumer<Object, LocalDate> tLocalDate = null;

        // Act
        WireIn actualDateResult = textValueIn.date(object, tLocalDate);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#demarshallable(Class)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInDemarshallable() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        Class clazz = null;

        // Act
        Demarshallable actualDemarshallableResult = textValueIn.demarshallable(clazz);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#float32()}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInFloat32() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;

        // Act
        float actualFloat32Result = textValueIn.float32();

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#float64()}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInFloat64() {
        System.setSecurityManager(new SecurityManager() {
            @Override
            public void checkPermission(Permission perm) {
                if (perm instanceof FilePermission)
                    return;
                if (perm instanceof PropertyPermission)
                    return;
                if (perm instanceof RuntimePermission)
                    return;
                if (perm instanceof ReflectPermission)
                    return;
                if (perm instanceof ManagementPermission)
                    return;
                if (perm.getName().equals("specifyStreamHandler"))
                    return;
                super.checkPermission(perm);
            }
        });
        YamlWire wire = new YamlWire();
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#float32(Object, ObjFloatConsumer)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInFloat322() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        Object object = null;
        ObjFloatConsumer<Object> tf = null;

        // Act
        WireIn actualFloat32Result = textValueIn.float32(object, tf);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#float64(Object, ObjDoubleConsumer)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInFloat642() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        Object object = null;
        ObjDoubleConsumer<Object> td = null;

        // Act
        WireIn actualFloat64Result = textValueIn.float64(object, td);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#float64()}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInFloat643() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access 'sun.misc'.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;

        // Act
        double actualFloat64Result = textValueIn.float64();

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#getADouble()}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInGetADouble() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;

        // Act
        double actualADouble = textValueIn.getADouble();

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#getALong()}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInGetALong() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;

        // Act
        long actualALong = textValueIn.getALong();

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#getBracketType()}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInGetBracketType() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;

        // Act
        BracketType actualBracketType = textValueIn.getBracketType();

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#hasNext()}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInHasNext() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;

        // Act
        boolean actualHasNextResult = textValueIn.hasNext();

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#hasNextSequenceItem()}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInHasNextSequenceItem() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;

        // Act
        boolean actualHasNextSequenceItemResult = textValueIn.hasNextSequenceItem();

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#int8()}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInInt8() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;

        // Act
        byte actualInt8Result = textValueIn.int8();

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#int16()}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInInt16() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;

        // Act
        short actualInt16Result = textValueIn.int16();

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#int32()}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInInt32() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;

        // Act
        int actualInt32Result = textValueIn.int32();

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#int64()}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInInt64() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;

        // Act
        long actualInt64Result = textValueIn.int64();

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#int64array(LongArrayValues, Object, BiConsumer)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInInt64array() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        LongArrayValues values = null;
        Object object = null;
        BiConsumer<Object, LongArrayValues> setter = null;

        // Act
        WireIn actualInt64arrayResult = textValueIn.int64array(values, object, setter);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#int8(Object, ObjByteConsumer)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInInt82() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        Object object = null;
        ObjByteConsumer<Object> tb = null;

        // Act
        WireIn actualInt8Result = textValueIn.int8(object, tb);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#int16(Object, ObjShortConsumer)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInInt162() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        Object object = null;
        ObjShortConsumer<Object> ti = null;

        // Act
        WireIn actualInt16Result = textValueIn.int16(object, ti);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#int32(Object, ObjIntConsumer)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInInt322() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        Object object = null;
        ObjIntConsumer<Object> ti = null;

        // Act
        WireIn actualInt32Result = textValueIn.int32(object, ti);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#int32(IntValue)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInInt323() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        IntValue value = null;

        // Act
        WireIn actualInt32Result = textValueIn.int32(value);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#int32(IntValue, Object, BiConsumer)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInInt324() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        IntValue value = null;
        Object object = null;
        BiConsumer<Object, IntValue> setter = null;

        // Act
        WireIn actualInt32Result = textValueIn.int32(value, object, setter);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#int64(Object, ObjLongConsumer)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInInt642() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        Object object = null;
        ObjLongConsumer<Object> tl = null;

        // Act
        WireIn actualInt64Result = textValueIn.int64(object, tl);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#int64(LongValue)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInInt643() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        LongValue value = null;

        // Act
        WireIn actualInt64Result = textValueIn.int64(value);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#int64(LongValue, Object, BiConsumer)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInInt644() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        LongValue value = null;
        Object object = null;
        BiConsumer<Object, LongValue> setter = null;

        // Act
        WireIn actualInt64Result = textValueIn.int64(value, object, setter);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#isNull()}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInIsNull() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;

        // Act
        boolean actualIsNullResult = textValueIn.isNull();

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#isTyped()}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInIsTyped() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;

        // Act
        boolean actualIsTypedResult = textValueIn.isTyped();

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#marshallable(Object, SerializationStrategy)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInMarshallable()
            throws BufferUnderflowException, IORuntimeException, InvalidMarshallableException {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        Object object = null;
        SerializationStrategy strategy = null;

        // Act
        Object actualMarshallableResult = textValueIn.marshallable(object, strategy);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#objectWithInferredType(Object, SerializationStrategy, Class)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInObjectWithInferredType() throws InvalidMarshallableException {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        Object object = null;
        SerializationStrategy strategy = null;
        Class type = null;

        // Act
        Object actualObjectWithInferredTypeResult = textValueIn.objectWithInferredType(object, strategy, type);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#objectWithInferredType0(Object, SerializationStrategy, Class)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInObjectWithInferredType0() throws InvalidMarshallableException {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        Object object = null;
        SerializationStrategy strategy = null;
        Class type = null;

        // Act
        Object actualObjectWithInferredType0Result = textValueIn.objectWithInferredType0(object, strategy, type);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#readLength()}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInReadLength() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;

        // Act
        long actualReadLengthResult = textValueIn.readLength();

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#readLengthMarshallable()}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInReadLengthMarshallable() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;

        // Act
        long actualReadLengthMarshallableResult = textValueIn.readLengthMarshallable();

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#readNumberOrText()}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInReadNumberOrText() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;

        // Act
        Object actualReadNumberOrTextResult = textValueIn.readNumberOrText();

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#resetState()}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInResetState() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;

        // Act
        textValueIn.resetState();

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#sequence(Object, Object, TriConsumer)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInSequence() throws InvalidMarshallableException {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        Object object = null;
        Object object2 = null;
        TriConsumer<Object, Object, ValueIn> tReader = null;

        // Act
        WireIn actualSequenceResult = textValueIn.sequence(object, object2, tReader);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#sequence(Object, BiConsumer)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInSequence2() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        Object object = null;
        BiConsumer<Object, ValueIn> tReader = null;

        // Act
        boolean actualSequenceResult = textValueIn.sequence(object, tReader);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#sequence(List, List, Supplier)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInSequence3() throws InvalidMarshallableException {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        List<Object> list = null;
        List<Object> buffer = null;
        Supplier<Object> bufferAdd = null;

        // Act
        boolean actualSequenceResult = textValueIn.sequence(list, buffer, bufferAdd);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#sequence(List, List, Supplier, ValueIn.Reader)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInSequence4() throws InvalidMarshallableException {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        List<Object> list = null;
        List<Object> buffer = null;
        Supplier<Object> bufferAdd = null;
        ValueIn.Reader reader0 = null;

        // Act
        boolean actualSequenceResult = textValueIn.sequence(list, buffer, bufferAdd, reader0);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#skipType()}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInSkipType() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;

        // Act
        textValueIn.skipType();

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#skipValue()}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInSkipValue() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;

        // Act
        WireIn actualSkipValueResult = textValueIn.skipValue();

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#stringForCode(int)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInStringForCode() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        int code = 0;

        // Act
        String actualStringForCodeResult = textValueIn.stringForCode(code);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#text()}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInText() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;

        // Act
        String actualTextResult = textValueIn.text();

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#textTo(StringBuilder)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInTextTo() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        StringBuilder sb = null;

        // Act
        StringBuilder actualTextToResult = textValueIn.textTo(sb);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#textTo0(StringBuilder)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInTextTo0() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        StringBuilder a = null;

        // Act
        StringBuilder actualTextTo0Result = textValueIn.textTo0(a);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#textTo(Bytes)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInTextTo2() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        Bytes<?> bytes = null;

        // Act
        Bytes<?> actualTextToResult = textValueIn.textTo(bytes);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#time(Object, BiConsumer)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInTime() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        Object object = null;
        BiConsumer<Object, LocalTime> setLocalTime = null;

        // Act
        WireIn actualTimeResult = textValueIn.time(object, setLocalTime);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#toString()}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInToString() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access 'sun.misc'.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;

        // Act
        String actualToStringResult = textValueIn.toString();

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#typeLiteral(BiFunction)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInTypeLiteral() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        BiFunction<CharSequence, ClassNotFoundException, Type> unresolvedHandler = null;

        // Act
        Type actualTypeLiteralResult = textValueIn.typeLiteral(unresolvedHandler);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#typeLiteralAsText(Object, BiConsumer)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInTypeLiteralAsText() throws BufferUnderflowException, IORuntimeException {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        Object object = null;
        BiConsumer<Object, CharSequence> classNameConsumer = null;

        // Act
        WireIn actualTypeLiteralAsTextResult = textValueIn.typeLiteralAsText(object, classNameConsumer);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#typePrefix()}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInTypePrefix() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;

        // Act
        Class actualTypePrefixResult = textValueIn.typePrefix();

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#typePrefix(Object, BiConsumer)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInTypePrefix2() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        Object object = null;
        BiConsumer<Object, CharSequence> ts = null;

        // Act
        ValueIn actualTypePrefixResult = textValueIn.typePrefix(object, ts);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#typePrefixOrObject(Class)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInTypePrefixOrObject() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        Class tClass = null;

        // Act
        Object actualTypePrefixOrObjectResult = textValueIn.typePrefixOrObject(tClass);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#typedMarshallable()}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInTypedMarshallable() throws InvalidMarshallableException {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;

        // Act
        Object actualTypedMarshallableResult = textValueIn.typedMarshallable();

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#uint8(Object, ObjShortConsumer)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInUint8() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        Object object = null;
        ObjShortConsumer<Object> ti = null;

        // Act
        WireIn actualUint8Result = textValueIn.uint8(object, ti);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#uint16()}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInUint16() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;

        // Act
        int actualUint16Result = textValueIn.uint16();

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#uint32(Object, ObjLongConsumer)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInUint32() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        Object object = null;
        ObjLongConsumer<Object> tl = null;

        // Act
        WireIn actualUint32Result = textValueIn.uint32(object, tl);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#uint16(Object, ObjIntConsumer)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInUint162() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        Object object = null;
        ObjIntConsumer<Object> ti = null;

        // Act
        WireIn actualUint16Result = textValueIn.uint16(object, ti);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#uuid(Object, BiConsumer)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInUuid() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        Object object = null;
        BiConsumer<Object, UUID> tuuid = null;

        // Act
        WireIn actualUuidResult = textValueIn.uuid(object, tuuid);

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#wireIn()}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInWireIn() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;

        // Act
        WireIn actualWireInResult = textValueIn.wireIn();

        // Assert
        // TODO: Add assertions on result
    }

    /**
     * Method under test: {@link YamlWire.TextValueIn#zonedDateTime(Object, BiConsumer)}
     */
    @Test
    @Ignore("TODO: Complete this test")
    public void testTextValueInZonedDateTime() {
        // TODO: Complete this test.
        //   Reason: R011 Sandboxing policy violation.
        //   Diffblue Cover ran code in your project that tried
        //     to access the network.
        //   Diffblue Cover's default sandboxing policy disallows this in order to prevent
        //   your code from damaging your system environment.
        //   See https://diff.blue/R011 to resolve this issue.

        // Arrange
        // TODO: Populate arranged inputs
        YamlWire.TextValueIn textValueIn = null;
        Object object = null;
        BiConsumer<Object, ZonedDateTime> tZonedDateTime = null;

        // Act
        WireIn actualZonedDateTimeResult = textValueIn.zonedDateTime(object, tZonedDateTime);

        // Assert
        // TODO: Add assertions on result
    }
}

