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

package net.openhft.chronicle.wire.channel;

/**
 * An extension of the {@link AbstractHeader} class designed to indicate successful establishment of a channel.
 *
 * <p>This class doesn't introduce new methods or fields. Its existence is primarily for type distinction, serving as a marker
 * to symbolize successful operations in the context of channel communication.
 */
public class OkHeader extends AbstractHeader<OkHeader> {
}
