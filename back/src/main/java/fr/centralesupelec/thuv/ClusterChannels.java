package fr.centralesupelec.thuv;

import io.grpc.ManagedChannel;

import java.util.HashMap;

/**
 * gRPC channels by cluster name, built once at startup from the clusters table.
 *
 * Cluster changes take effect at the next back end restart: a cluster created or renamed since
 * startup has no channel here, and an edited address keeps the old connection. Looking up a
 * missing cluster fails with that explanation instead of a null channel.
 */
public class ClusterChannels extends HashMap<String, ManagedChannel> {
    @Override
    public ManagedChannel get(Object clusterName) {
        ManagedChannel channel = super.get(clusterName);
        if (channel == null) {
            throw new IllegalStateException(String.format(
                    "No gRPC channel for cluster %s: cluster changes take effect after the back end restarts",
                    clusterName
            ));
        }
        return channel;
    }
}
