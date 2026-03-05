package wood.mike.sbminikubelet.unit.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ListContainersCmd;
import com.github.dockerjava.api.command.SyncDockerCmd;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.command.ListContainersCmdImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import wood.mike.config.EtcdProperties;
import wood.mike.model.ContainerStatus;
import wood.mike.sbminikubelet.service.StatusReporterService;
import wood.mike.service.EtcdService;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StatusReporterServiceTest {

    @Mock
    private DockerClient dockerClient;
    @Mock
    private EtcdService etcdService;
    private EtcdProperties etcdProperties;
    private String nodeName = "node-1";
    private ObjectMapper objectMapper = new ObjectMapper();

    private StatusReporterService statusReporterService;

    @BeforeEach
    public void setUp() {
        etcdProperties = new EtcdProperties(
                "http://localhost:2379",
                "/registry/containers/specs/",
                "/registry/containers/status/",
                "/registry/nodes/status/",
                5L,
                20L
        );

        statusReporterService = new StatusReporterService(dockerClient, etcdService, etcdProperties, nodeName);
    }

    @Test
    public void testReportStatus() {
        ListContainersCmd.Exec exec = command -> {
            String jsonContainer = """
                        {
                          "Names": ["webserver"],
                          "State": "running",
                          "ImageID": "fd204fe2f750",
                          "Status": "Up 19 hours",
                          "Labels": {
                            "managed-by": "kubelight",
                            "kubelight.node.name": "node-1"
                          }
                        }
                        """;
            return List.of(objectMapper.readValue(jsonContainer, Container.class));
        };

        when(dockerClient.listContainersCmd()).thenReturn(new ListContainersCmdImpl(exec));
        statusReporterService.reportStatus();
        verify(etcdService, times(1)).put(anyString(), any(ContainerStatus.class));
    }
}
