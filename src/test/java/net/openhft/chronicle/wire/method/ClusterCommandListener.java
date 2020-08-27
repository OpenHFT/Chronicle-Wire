package net.openhft.chronicle.wire.method;

public interface ClusterCommandListener {
    void command(ClusterCommand clusterCommand);
}