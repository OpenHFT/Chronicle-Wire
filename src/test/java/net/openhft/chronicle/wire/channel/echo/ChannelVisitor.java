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

package net.openhft.chronicle.wire.channel.echo;

import net.openhft.chronicle.wire.SelfDescribingMarshallable;

/**
 * Abstract class 'ChannelVisitor' serves as a template for creating objects
 * that can visit a ChronicleChannel and perform an operation,
 * returning a result of type T. Extends SelfDescribingMarshallable for serialization support.
 *
 * @param <T> the type of result to return after visiting a ChronicleChannel.
 */
@SuppressWarnings("deprecation")
public abstract class ChannelVisitor<T> extends SelfDescribingMarshallable {

    /**
     * Abstract method designed to be implemented by subclasses to define
     * the operation to be performed on the visited ChronicleChannel.
     *
     * @param channel the ChronicleChannel to visit.
     * @return T result of the operation performed on the channel.
     */
    public abstract T visit(net.openhft.chronicle.wire.channel.ChronicleChannel channel);
}
