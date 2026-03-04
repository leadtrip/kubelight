package wood.mike.sbetcd.unit.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import wood.mike.config.EtcdProperties;
import wood.mike.model.ContainerSpec;
import wood.mike.model.NodeStatus;
import wood.mike.sbetcd.service.NodeMonitorService;
import wood.mike.sbetcd.service.SchedulerService;
import wood.mike.service.EtcdService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NodeMonitorServiceTest {

    private NodeMonitorService nodeMonitorService;

    @Mock
    private EtcdService etcdService;

    @Mock
    private SchedulerService schedulerService;

    private EtcdProperties etcdProperties;

    @BeforeEach
    void setup() {
        etcdProperties = new EtcdProperties(
                "http://localhost:2379",
                "/registry/containers/specs/",
                "/registry/containers/status/",
                "/registry/nodes/status/",
                5L,
                20L
        );

        nodeMonitorService = new NodeMonitorService(etcdService, etcdProperties, schedulerService);
    }

    @Test
    void testRescueOrphanedContainers() {
        String node2Key = "/registry/containers/specs/node-2/whoami";

        when(etcdService.listPrefix(etcdProperties.nodeStatusPrefix(), NodeStatus.class)).thenReturn(List.of(new NodeStatus("node-1", "Ready", System.currentTimeMillis(), "1.0.0")));
        when(etcdService.listKeys(etcdProperties.containerPrefix())).thenReturn(List.of("/registry/containers/specs/node-1/web-server", node2Key));
        when(etcdService.getValue(node2Key, ContainerSpec.class)).thenReturn(Optional.of(new ContainerSpec("whoami", "traefi/whoami:latest", 8081, 80)));

        nodeMonitorService.rescueOrphanedContainers();

        verify(schedulerService).scheduleContainer(any(ContainerSpec.class));
        verify(etcdService).delete(node2Key);
    }

    @Test
    void testExtractNodeFromKey() {
        String key = "/registry/containers/specs/node-1/nginx";
        String node = nodeMonitorService.extractNodeFromKey(key);
        assertEquals("node-1", node);

        assertNull(nodeMonitorService.extractNodeFromKey("/no/node/key"));
    }
}
