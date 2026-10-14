package fr.centralesupelec.thuv.service;

import fr.centralesupelec.thuv.ClusterChannels;
import fr.centralesupelec.thuv.dtos.ClusterUpdateDto;
import fr.centralesupelec.thuv.mappers.ClusterMapper;
import fr.centralesupelec.thuv.model.Cluster;
import fr.centralesupelec.thuv.repository.ClusterRepository;
import io.grpc.ManagedChannel;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClusterRestartLifecycleTest {
    private final ClusterRepository clusterRepository = mock(ClusterRepository.class);
    private final ClusterService service = new ClusterService(clusterRepository, mock(ClusterMapper.class));

    @Test
    void aClusterCreatedSinceStartupAsksForARestart() {
        ClusterChannels channels = new ClusterChannels();
        ManagedChannel known = mock(ManagedChannel.class);
        channels.put("a", known);

        assertThat(channels.get("a")).isSameAs(known);
        assertThatThrownBy(() -> channels.get("b"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cluster b")
                .hasMessageContaining("restart");
    }

    @Test
    void creatingATakenNameIsAConflict() {
        when(clusterRepository.existsByName("a")).thenReturn(true);

        assertThatThrownBy(() -> service.updateCluster(dto("a"), new Cluster()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(clusterRepository, never()).saveAndFlush(any());
    }

    @Test
    void renamingOntoAnotherClusterIsAConflictButKeepingItsOwnNameIsNot() {
        Cluster existing = new Cluster();
        existing.setId(1L);
        when(clusterRepository.existsByNameAndIdNot("b", 1L)).thenReturn(true);
        when(clusterRepository.existsByNameAndIdNot("a", 1L)).thenReturn(false);

        assertThatThrownBy(() -> service.updateCluster(dto("b"), existing))
                .isInstanceOf(ResponseStatusException.class);
        service.updateCluster(dto("a"), existing);
        verify(clusterRepository).saveAndFlush(existing);
    }

    private static ClusterUpdateDto dto(String name) {
        ClusterUpdateDto dto = new ClusterUpdateDto();
        dto.setName(name);
        return dto;
    }
}
