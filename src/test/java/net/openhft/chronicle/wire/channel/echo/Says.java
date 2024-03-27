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

import net.openhft.chronicle.core.io.Syncable;

/**
 * The {@code Says} interface outlines a communication contract
 * for entities capable of sending messages within the system.
 * It extends {@code Syncable}, meaning implementations may
 * require synchronization capabilities.
 */
public interface Says extends Syncable {
    /**
     * Sends a message, represented by the provided {@code String}.
     *
     * @param say The message to be sent or communicated.
     */
    void say(String say);
}
