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

/**
 * TestOut interface defines methods for outputting event data and error messages.
 */
public interface TestOut {

    /**
     * Outputs a processed TestEvent object.
     *
     * @param dto The processed TestEvent data transfer object.
     */
    void testEvent(TestEvent dto);

    /**
     * Outputs an error message.
     *
     * @param message The error message to be outputted.
     */
    void error(String message);

    /**
     * Outputs a processed TestAbstractMarshallableCfgEvent object.
     *
     * @param dto The processed TestAbstractMarshallableCfgEvent data transfer object.
     */
    void testAbstractMarshallableCfgEvent(TestAbstractMarshallableCfgEvent dto);
}
