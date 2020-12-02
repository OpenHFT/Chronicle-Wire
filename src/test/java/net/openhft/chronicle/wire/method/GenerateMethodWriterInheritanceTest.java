package net.openhft.chronicle.wire.method;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.MethodId;
import net.openhft.chronicle.bytes.MethodReader;
import net.openhft.chronicle.wire.*;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.openhft.chronicle.wire.WireType.BINARY;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GenerateMethodWriterInheritanceTest {

    @Test
    public void testSameClassInHierarchy() {
        final Wire wire = BINARY.apply(Bytes.elasticByteBuffer());

        final AnInterface writer = wire.methodWriter(AnInterface.class, ADescendant.class);
        assertTrue(writer instanceof MethodWriter);

        writer.sayHello("hello world");

        final AtomicBoolean callRegistered = new AtomicBoolean();

        final MethodReader reader = wire.methodReader(new SameInterfaceImpl(callRegistered));

        assertTrue(reader.readOne());
        assertTrue(callRegistered.get());

        // VanillaMethodReader is used in case compilation of generated reader failed.
        assertFalse(reader instanceof VanillaMethodReader);

        // Proxy method writer is constructed in case compilation of generated writer failed.
        assertFalse(Proxy.isProxyClass(writer.getClass()));
    }

    @Test
    public void testSameNamedMethod() {
        final Wire wire = BINARY.apply(Bytes.elasticByteBuffer());

        final AnInterface writer = wire.methodWriter(AnInterface.class, AnInterfaceSameName.class);
        assertTrue(writer instanceof MethodWriter);

        writer.sayHello("hello world");

        final AtomicBoolean callRegistered = new AtomicBoolean();

        final MethodReader reader = wire.methodReader(new SameMethodNameImpl(callRegistered));

        assertTrue(reader.readOne());
        assertTrue(callRegistered.get());

        // VanillaMethodReader is used in case compilation of generated reader failed.
        assertFalse(reader instanceof VanillaMethodReader);

        // Proxy method writer is constructed in case compilation of generated writer failed.
        assertFalse(Proxy.isProxyClass(writer.getClass()));
    }

    // TODO: same names but different MethodIds should barf

    @Test(expected = MethodWriterValidationException.class)
    public void testDuplicateMethodIds() {
        final Wire wire = BINARY.apply(Bytes.elasticByteBuffer());

        final VanillaMethodWriterBuilder<AnInterfaceMethodId> builder = (VanillaMethodWriterBuilder<AnInterfaceMethodId>) wire.methodWriterBuilder(AnInterfaceMethodId.class);
        builder.addInterface(AnInterfaceSameMethodId.class).useMethodIds(true).build();
    }

    @Test(expected = MethodWriterValidationException.class)
    public void testGenerateForClass() {
        final Wire wire = BINARY.apply(Bytes.elasticByteBuffer());

        wire.methodWriter(GenerateMethodWriterInheritanceTest.class);
    }

    interface AnInterface {
        void sayHello(String name);
    }

    interface AnInterfaceSameName {
        void sayHello(String name);
    }

    interface AnInterfaceMethodId {
        @MethodId(1)
        void sayHello(String name);
    }

    interface AnInterfaceSameMethodId {
        @MethodId(1)
        void sayHello2(String name);
    }

    interface ADescendant extends AnInterface {
    }

    static class SameMethodNameImpl implements AnInterface, AnInterfaceSameName {
        private final AtomicBoolean callRegistered;

        public SameMethodNameImpl(AtomicBoolean callRegistered) {
            this.callRegistered = callRegistered;
        }

        @Override
        public void sayHello(String name) {
            callRegistered.set(true);
        }
    }

    static class SameInterfaceImpl implements AnInterface, ADescendant {
        private final AtomicBoolean callRegistered;

        public SameInterfaceImpl(AtomicBoolean callRegistered) {
            this.callRegistered = callRegistered;
        }

        @Override
        public void sayHello(String name) {
            callRegistered.set(true);
        }
    }
}

