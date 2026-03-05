package wood.mike.sbetcd.unit.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import wood.mike.config.EtcdProperties;
import wood.mike.model.ContainerSpec;
import wood.mike.model.NodeStatus;
import wood.mike.sbetcd.service.SchedulerService;
import wood.mike.service.EtcdService;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SchedulerServiceTest {

    @Mock
    private EtcdService etcdService;

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

        schedulerService = new SchedulerService(etcdService, etcdProperties);
    }

    @Test
    public void testScheduleContainer() {
        var nodeName = "node-1";
        List<NodeStatus> nodeList = List.of(new NodeStatus(nodeName, "Ready", System.currentTimeMillis(), "1.0.0"));
        when(etcdService.listPrefix(etcdProperties.nodeStatusPrefix(), NodeStatus.class)).thenReturn(nodeList);
        ContainerSpec containerSpec = new ContainerSpec("webserver", "nginx:latest", 8080, 80);
        schedulerService.scheduleContainer(containerSpec);

        String destinationKey = etcdProperties.containerPrefix() + nodeName + "/" + containerSpec.name();
        verify(etcdService, times(1)).put(destinationKey, containerSpec);
    }
}
