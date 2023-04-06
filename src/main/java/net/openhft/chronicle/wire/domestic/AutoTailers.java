/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire.domestic;

import net.openhft.chronicle.core.io.InvalidMarshallableException;
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
                                  @NotNull final ExcerptListener excerptListener) throws InvalidMarshallableException {
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