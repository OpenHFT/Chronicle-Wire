//
// Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
//
package net.openhft.chronicle.wire.method;

/**
 * An interface defining a listener for cluster commands.
 * Implementations of this interface can process commands intended for cluster operations.
 */
public interface ClusterCommandListener {

    /**
     * Invoked when a cluster command needs to be processed.
     *
     * @param clusterCommand The cluster command to be processed.
     */
    void command(ClusterCommand clusterCommand);
}
