package fr.centralesupelec.thuv.service;

import fr.centralesupelec.thuv.ClusterChannels;
import fr.centralesupelec.thuv.dtos.ClusterUpdateDto;
import fr.centralesupelec.thuv.mappers.ClusterMapper;
import fr.centralesupelec.thuv.model.Cluster;
import fr.centralesupelec.thuv.repository.ClusterRepository;
import io.grpc.ManagedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Cluster changes apply at restart: until then a changed or deleted cluster is unreachable, a new
// one has no channel, and a running name cannot be handed to another cluster.
class ClusterRestartLifecycleTest {
    private final ClusterRepository clusterRepository = mock(ClusterRepository.class);
    private final ClusterChannels channels = new ClusterChannels();
    private final ManagedChannel channelA = mock(ManagedChannel.class);
    private ClusterService service;
    private Cluster clusterA;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ObjectProvider<ClusterChannels> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(channels);
        service = new ClusterService(clusterRepository, mock(ClusterMapper.class), provider);
        clusterA = cluster(1L, "a", "10.0.0.1", 50000);
        channels.register(clusterA, channelA);
    }

    @Test
    void aClusterCreatedSinceStartupHasNoChannel() {
        service.updateCluster(dto("b", "10.0.0.2", 50000), new Cluster());

        assertThat(channels.get("a")).isSameAs(channelA);
        assertThatThrownBy(() -> channels.get("b")).hasMessageContaining("restarts");
    }

    @Test
    void creatingATakenNameIsAConflict() {
        when(clusterRepository.existsByName("x")).thenReturn(true);

        assertThatThrownBy(() -> service.updateCluster(dto("x", "10.0.0.2", 50000), new Cluster()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(clusterRepository, never()).saveAndFlush(any());
    }

    @Test
    void renamingMakesTheClusterUnreachableAndItsOldNameCannotBeReused() {
        service.updateCluster(dto("c", "10.0.0.1", 50000), clusterA);

        assertThatThrownBy(() -> channels.get("a")).hasMessageContaining("restarts");
        assertThatThrownBy(() -> service.updateCluster(dto("a", "10.0.0.9", 50000), new Cluster()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("until the back end restarts");
    }

    @Test
    void editingTheEndpointMakesTheClusterUnreachable() {
        service.updateCluster(dto("a", "10.0.0.7", 50000), clusterA);

        assertThatThrownBy(() -> channels.get("a")).hasMessageContaining("restarts");
    }

    @Test
    void editingThePortMakesTheClusterUnreachable() {
        service.updateCluster(dto("a", "10.0.0.1", 50001), clusterA);

        assertThatThrownBy(() -> channels.get("a")).hasMessageContaining("restarts");
    }

    @Test
    void anEditThatKeepsTheEndpointKeepsTheChannel() {
        ClusterUpdateDto sameEndpoint = dto("a", "10.0.0.1", 50000);
        sameEndpoint.setDeploymentEnabled(true);

        service.updateCluster(sameEndpoint, clusterA);

        assertThat(channels.get("a")).isSameAs(channelA);
    }

    @Test
    void deletingMakesTheClusterUnreachable() {
        service.deleteCluster(clusterA);

        assertThatThrownBy(() -> channels.get("a")).hasMessageContaining("restarts");
    }

    private static ClusterUpdateDto dto(String name, String address, int port) {
        ClusterUpdateDto dto = new ClusterUpdateDto();
        dto.setName(name);
        dto.setGoAddress(address);
        dto.setGoPort(port);
        return dto;
    }

    private static Cluster cluster(Long id, String name, String address, int port) {
        Cluster cluster = new Cluster();
        cluster.setId(id);
        cluster.setName(name);
        cluster.setGoAddress(address);
        cluster.setGoPort(port);
        return cluster;
    }
}
