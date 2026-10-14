package fr.centralesupelec.thuv;

import fr.centralesupelec.thuv.model.Cluster;
import io.grpc.ManagedChannel;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * gRPC channels by cluster name, built once at startup from the clusters table.
 *
 * <p>Cluster changes take effect at the next back end restart. A cluster created since startup has
 * no channel; one whose name, address or port was edited, or that was deleted, has its channel
 * marked stale. Looking either up fails with that explanation instead of reaching the old
 * endpoint. A name keeps belonging to its startup cluster until the restart, so it cannot be
 * reused meanwhile.
 */
public class ClusterChannels extends HashMap<String, ManagedChannel> {
    private final Map<String, Long> owners = new ConcurrentHashMap<>();
    private final Set<String> stale = ConcurrentHashMap.newKeySet();

    public void register(Cluster cluster, ManagedChannel channel) {
        put(cluster.getName(), channel);
        owners.put(cluster.getName(), cluster.getId());
    }

    /** The id of the cluster this name belonged to at startup, or null. */
    public Long ownerOf(String clusterName) {
        return owners.get(clusterName);
    }

    public void markStale(String clusterName) {
        stale.add(clusterName);
    }

    @Override
    public ManagedChannel get(Object clusterName) {
        ManagedChannel channel = super.get(clusterName);
        if (channel == null || stale.contains(clusterName)) {
            throw new IllegalStateException(String.format(
                    "No current gRPC channel for cluster %s: cluster changes take effect after the back end restarts",
                    clusterName
            ));
        }
        return channel;
    }
}
