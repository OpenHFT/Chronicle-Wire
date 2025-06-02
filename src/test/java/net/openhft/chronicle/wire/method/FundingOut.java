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
 * Interface that combines the functionalities of both FundingListener and ClusterCommandListener.
 * It represents an entity capable of handling both funding-related events and cluster command events.
 * This design allows a single implementation to listen to and process a variety of different event types.
 */
public interface FundingOut extends
        FundingListener, ClusterCommandListener {
    // This interface currently does not declare any additional methods, but it inherits all methods
    // from both FundingListener and ClusterCommandListener.
}
