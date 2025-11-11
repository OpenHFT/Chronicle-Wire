/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.wire.method;

/**
 * Interface that combines the functionalities of both FundingListener and ClusterCommandListener.
 * It represents an entity capable of handling both funding-related events and cluster command events.
 * This design allows a single implementation to listen to and process a variety of different event types.
 */
interface FundingOut extends
        FundingListener, ClusterCommandListener {
    // This interface currently does not declare any additional methods, but it inherits all methods
    // from both FundingListener and ClusterCommandListener.
}
