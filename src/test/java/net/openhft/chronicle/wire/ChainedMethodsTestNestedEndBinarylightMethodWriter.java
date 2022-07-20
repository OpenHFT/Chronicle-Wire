package net.openhft.chronicle.wire;

// NOTE: Added to check backward compatibility

import net.openhft.chronicle.bytes.UpdateInterceptor;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.Closeable;

import java.util.function.Supplier;

public final class ChainedMethodsTestNestedEndBinarylightMethodWriter implements ChainedMethodsTest.NestedEnd, MethodWriter {

    // result
    private transient final Closeable closeable;
    private transient Supplier<MarshallableOut> out;

    // constructor
    public ChainedMethodsTestNestedEndBinarylightMethodWriter(Supplier<MarshallableOut> out, Closeable closeable, UpdateInterceptor updateInterceptor) {
        this.out = out;
        this.closeable = closeable;
    }

    @Override
    public void marshallableOut(MarshallableOut out) {
        this.out = () -> out;
    }

    public void end() {
        try (final WriteDocumentContext dc = (WriteDocumentContext) this.out.get().acquireWritingDocument(false)) {
            try {
                dc.chainedElement(false);
                if (out.get().recordHistory()) MessageHistory.writeHistory(dc);
                final ValueOut valueOut = dc.wire().writeEventName("end");
                valueOut.text("");
            } catch (Throwable t) {
                dc.rollbackOnClose();
                throw Jvm.rethrow(t);
            }
        }
    }

}
