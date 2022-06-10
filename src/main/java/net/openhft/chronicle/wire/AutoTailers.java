package net.openhft.chronicle.wire;

import net.openhft.chronicle.core.threads.EventHandler;
import net.openhft.chronicle.threads.Pauser;
import net.openhft.chronicle.wire.ExcerptListener;
import net.openhft.chronicle.wire.MarshallableIn;
import net.openhft.chronicle.wire.internal.InternalAutoTailers;
import net.openhft.chronicle.wire.internal.reduction.ReductionUtil;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

import static net.openhft.chronicle.core.util.ObjectUtils.requireNonNull;

public final class AutoTailers {

    // Suppresses default constructor, ensuring non-instantiability.
    private AutoTailers() {
    }

    public interface CloseableRunnable extends Runnable, AutoCloseable {
        @Override
        void close();
    }

    public interface CloseableEventHandler extends EventHandler, AutoCloseable {
        @Override
        void close();
    }

    public static long replayOnto(@NotNull final MarshallableIn tailer,
                                  @NotNull final ExcerptListener excerptListener) {
        requireNonNull(tailer);
        requireNonNull(excerptListener);

        return ReductionUtil.accept(tailer, excerptListener);
    }

    @NotNull
    public static CloseableRunnable createRunnable(@NotNull final Supplier<? extends MarshallableIn> tailerSupplier,
                                                   @NotNull final ExcerptListener excerptListener,
                                                   @NotNull final Supplier<Pauser> pauserSupplier) {
        requireNonNull(tailerSupplier);
        requireNonNull(excerptListener);
        requireNonNull(pauserSupplier);

        return new InternalAutoTailers.RunnablePoller(tailerSupplier, excerptListener, pauserSupplier);
    }

    @NotNull
    public static CloseableEventHandler createEventHandler(@NotNull final Supplier<? extends MarshallableIn> tailerSupplier,
                                                           @NotNull final ExcerptListener excerptListener) {
        requireNonNull(tailerSupplier);
        requireNonNull(excerptListener);

        return new InternalAutoTailers.EventHandlerPoller(tailerSupplier, excerptListener);
    }

}