/*
 * Copyright 2014 Higher Frequency Trading
 *
 * http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.wire.set;

import net.openhft.chronicle.wire.*;

import java.io.StreamCorruptedException;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static net.openhft.chronicle.wire.set.SetWireHandler.Params.key;
import static net.openhft.chronicle.wire.set.SetWireHandler.Params.segment;

/**
 * @param <O> the collection type
 * @param <E> the type of each element in that collection
 */
public interface SetWireHandler<O, E> {

    void process(Wire in,
                 Wire out,
                 O set,
                 CharSequence csp,
                 BiConsumer< ValueOut,E> toWire,
                 Function<ValueIn, E> fromWire) throws StreamCorruptedException;


    enum Params implements WireKey {
        key,
        segment,
    }

    enum SetEventId implements ParameterizeWireKey {
        size,
        isEmpty,
        add,
        addAll,
        retainAll,
        containsAll,
        removeAll,
        clear,
        remove(key),
        numberOfSegments,
        contains(key),
        iterator(segment);

        private final WireKey[] params;

        <P extends WireKey> SetEventId(P... params) {
            this.params = params;
        }

        public <P extends WireKey> P[] params() {
            return (P[]) this.params;

        }
    }
}
