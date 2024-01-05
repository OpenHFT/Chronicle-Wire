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

/**
 * The {@code Replies} interface defines a contract for entities
 * capable of providing replies or responses in the system.
 */
public interface Replies {
    /**
     * Replies or responds with the provided object. The nature of
     * the reply (e.g., whether it is sent to a specific destination)
     * may depend on the implementing class.
     *
     * @param o The object to be sent as a reply.
     */
    void reply(Object o);
}
