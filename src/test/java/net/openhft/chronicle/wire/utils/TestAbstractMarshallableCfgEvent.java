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

package net.openhft.chronicle.wire.utils;

import net.openhft.chronicle.wire.AbstractMarshallableCfg;

/**
 * Defines a test event class that extends from AbstractMarshallableCfg. This class is
 * primarily used for testing purposes, leveraging the serialization and deserialization
 * capabilities provided by the AbstractMarshallableCfg.
 */
public class TestAbstractMarshallableCfgEvent extends AbstractMarshallableCfg {

    // Represents a numerical value associated with this event. The 'number' field is of type long,
    // suggesting it can store large integer values. This field's specific usage or purpose
    // would typically depend on the context in which the TestAbstractMarshallableCfgEvent is used.
    private long number;
}
