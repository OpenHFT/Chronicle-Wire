package net.openhft.chronicle.wire.internal.streaming;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.TextWire;
import net.openhft.chronicle.wire.ValueOut;
import net.openhft.chronicle.wire.Wire;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public final class CreateUtil {

    private CreateUtil() {
    }

    @SafeVarargs
    @NotNull
    public static Wire createThenValueOuts(@NotNull final Consumer<ValueOut>... mutators) {
        final Consumer<Wire> queueMutator = wire -> {
            for (Consumer<ValueOut> mutator : mutators) {
                try (DocumentContext dc = wire.writingDocument()) {
                    mutator.accept(dc.wire().getValueOut());
                }
            }
        };
        return createThen(queueMutator);
    }

    @SafeVarargs
    @NotNull
    public static Wire createThenWritingDocuments(@NotNull final Consumer<DocumentContext>... mutators) {
        final Consumer<Wire> wireMutator = wire -> {
            for (Consumer<DocumentContext> mutator : mutators) {
                try (DocumentContext dc = wire.writingDocument()) {
                    mutator.accept(dc);
                }
            }
        };
        return createThen(wireMutator);
    }
/*

    @NotNull
    public static Wire createThenAppending(@NotNull final String name,
                                           @NotNull final Consumer<? super Wire> mutator) {
        final Consumer<SingleChronicleQueue> queueMutator = q -> {
            ExcerptAppender excerptAppender = q.acquireAppender();
            mutator.accept(excerptAppender);
        };
        return createThen(name, queueMutator);
    }
*/

    @NotNull
    public static Wire createThen(@NotNull final Consumer<? super Wire> wireMutator) {
        final Wire wire = create();
        wireMutator.accept(wire);
        return wire;
    }

    @NotNull
    public static Wire create() {
        return new TextWire(Bytes.elasticByteBuffer());
    }

}