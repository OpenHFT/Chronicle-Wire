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

package net.openhft.chronicle.wire.domestic.streaming;

import net.openhft.chronicle.wire.DocumentContext;
import net.openhft.chronicle.wire.ValueOut;
import net.openhft.chronicle.wire.Wire;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

// Utility class for creating and manipulating Wire objects
public final class CreateUtil {

    // Private constructor to prevent instantiation of utility class
    private CreateUtil() {
    }

    // Create a Wire and then apply a series of ValueOut mutators to it
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

    // Create a Wire and then apply a series of DocumentContext mutators to it
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

    // [Commented Out Code: Create a Wire by appending it to a named Chronicle queue]
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

    // Create a Wire and then apply a single Wire mutator to it
    @NotNull
    public static Wire createThen(@NotNull final Consumer<? super Wire> wireMutator) {
        final Wire wire = create();
        wireMutator.accept(wire);
        return wire;
    }

    // Create and return a new instance of a Wire with a YAML format stored on the heap
    @NotNull
    public static Wire create() {
        return Wire.newYamlWireOnHeap();
    }
}
