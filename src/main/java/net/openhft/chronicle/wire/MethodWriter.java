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

package net.openhft.chronicle.wire;

/**
 * Defines a contract for components that can serialise method calls to a {@link MarshallableOut} target.
 * Typically implemented by proxies created via {@link MarshallableOut#methodWriter(Class, Class...)} or
 * {@link VanillaMethodWriterBuilder}. Implementations should ensure that method
 * invocations on the writer result in corresponding messages being written to the configured {@code MarshallableOut}.
 *
 * @see MarshallableOut#methodWriter(Class, Class...)
 * @see VanillaMethodWriterBuilder
 */
public interface MethodWriter {

    /**
     * Re-targets this writer to the given {@link MarshallableOut}.
     * Subsequent method calls will be serialised to that destination and the writer's
     * internal state is typically updated to reflect the new sink.
     *
     * @param out new sink for future method call serialisations; must not be null
     */
    void marshallableOut(MarshallableOut out);
}
