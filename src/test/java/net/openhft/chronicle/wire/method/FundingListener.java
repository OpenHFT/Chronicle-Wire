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

package net.openhft.chronicle.wire.method;

/**
 * Interface defining a listener for funding-related events.
 * It provides methods for handling different types of funding notifications.
 */
public interface FundingListener {

    /**
     * Handles an event with detailed funding information.
     * This method is called when there is a funding event with a full Funding object.
     *
     * @param funding The Funding object containing detailed information about the funding event.
     */
    void funding(Funding funding);

    /**
     * Handles a funding event represented as a primitive integer.
     * This method can be used for simpler notifications where the funding information
     * is represented as a single integer value.
     *
     * @param num The integer representation of the funding information.
     */
    void fundingPrimitive(int num);

    /**
     * Handles a funding event with no additional arguments.
     * This method can be used for notifications where the event itself is sufficient
     * and no additional data is needed.
     */
    void fundingNoArg();
}
