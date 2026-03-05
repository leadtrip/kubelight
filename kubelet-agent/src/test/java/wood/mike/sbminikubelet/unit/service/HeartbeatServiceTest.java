package wood.mike.sbminikubelet.unit.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import wood.mike.config.EtcdProperties;
import wood.mike.sbminikubelet.service.HeartbeatService;
import wood.mike.service.EtcdService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class HeartbeatServiceTest {
    @Mock
    private EtcdService etcdService;
    private EtcdProperties etcdProperties;
    private HeartbeatService heartbeatService;

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

        heartbeatService = new HeartbeatService(etcdService, etcdProperties, "node-1");
    }

    @Test
    public void testRun() {
        heartbeatService.run();
        verify(etcdService, times(1)).putWithLease(anyString(), any(), anyLong());
    }
}
