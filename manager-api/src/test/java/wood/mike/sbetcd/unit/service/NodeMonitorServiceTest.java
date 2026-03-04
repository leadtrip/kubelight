package wood.mike.sbetcd.unit.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import wood.mike.config.EtcdProperties;
import wood.mike.sbetcd.service.NodeMonitorService;
import wood.mike.sbetcd.service.SchedulerService;
import wood.mike.service.EtcdService;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class NodeMonitorServiceTest {

    private NodeMonitorService nodeMonitorService;

    @Mock
    private EtcdService etcdService;

    @Mock
    private SchedulerService schedulerService;

    @BeforeEach
    void setup() {
        // Manually create your record with the test paths
        // No @Autowired here!
        EtcdProperties etcdProperties = new EtcdProperties(
                "http://localhost:2379",
                "/registry/containers/specs/",
                "/registry/containers/status/",
                "/registry/nodes/status/",
                5L,
                20L
        );

        // Manually inject because Mockito's @InjectMocks
        // sometimes struggles with Records/Constructor injection
        nodeMonitorService = new NodeMonitorService(etcdService, etcdProperties, schedulerService);
    }

    @Test
    void testExtractNodeFromKey() {
        String key = "/registry/containers/specs/node-1/nginx";
        String node = nodeMonitorService.extractNodeFromKey(key);
        assertEquals("node-1", node);
    }
}
