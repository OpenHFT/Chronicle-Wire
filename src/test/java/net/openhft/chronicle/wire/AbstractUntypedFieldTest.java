package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AbstractUntypedFieldTest {

   @BeforeEach
   void beforeEach() {
       ClassAliasPool.CLASS_ALIASES.addAlias(AImpl.class, "AImpl");
   }

    @Test
    void typedFieldsShouldBeNonNull() {
        final Bytes<?> bytes = Bytes.from("!net.openhft.chronicle.wire.AbstractUntypedFieldShouldBeNull$Holder {\n" +
                "  a: !AImpl {\n" +
                "  }\n" +
                "}");
        final TextWire textWire = new TextWire(bytes);

        final Holder holder = textWire.getValueIn().object(Holder.class);

        assertNotNull(holder.a);
    }

    @Test
    void untypedFieldsShouldBeNull() {
        final Bytes<?> bytes = Bytes.from("!net.openhft.chronicle.wire.AbstractUntypedFieldShouldBeNull$Holder {\n" +
                "  a: {\n" +
                "  }\n" +
                "}");
        final TextWire textWire = new TextWire(bytes);

        final Holder holder = textWire.getValueIn().object(Holder.class);

        assertNull(holder.a);
    }

    static abstract class A {
    }

    private static final class AImpl extends A {
    }

    private static final class Holder {
        A a;
    }

}
