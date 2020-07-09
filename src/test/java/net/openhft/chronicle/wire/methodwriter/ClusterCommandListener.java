package net.openhft.chronicle.wire.methodwriter;

public interface ClusterCommandListener {
    void command(ClusterCommand clusterCommand);
}