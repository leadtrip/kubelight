package wood.mike.sbminikubelet.unit.service;

import com.google.protobuf.ByteString;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.test.EtcdClusterExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import tools.jackson.databind.ObjectMapper;
import wood.mike.config.EtcdProperties;
import wood.mike.model.ContainerSpec;
import wood.mike.sbminikubelet.service.ContainerService;
import wood.mike.sbminikubelet.service.KubeletReconcilerService;
import wood.mike.service.EtcdService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class KubeletReconcilerServiceTest {

    @RegisterExtension
    private static final EtcdClusterExtension cluster = EtcdClusterExtension.builder()
            .withNodes(1)
            .build();

    @Mock
    private EtcdService etcdService;
    @Mock
    private ContainerService containerService;
    private final ObjectMapper objectMapper =  new ObjectMapper();
    private final String nodeName = "node-1";
    private String assignementPrefix;

    private KubeletReconcilerService kubeletReconcilerService;

    @BeforeEach
    public void setUp() {
        EtcdProperties etcdProperties = new EtcdProperties(
                "http://localhost:2379",
                "/registry/containers/specs/",
                "/registry/containers/status/",
                "/registry/nodes/status/",
                5L,
                20L
        );
        String endpoint = cluster.clientEndpoints().get(0).toString();
        Client client = Client.builder().endpoints(endpoint).build();
        etcdService = new EtcdService(client, new ObjectMapper(), etcdProperties);
        assignementPrefix = etcdProperties.containerPrefix() + nodeName + "/";

        kubeletReconcilerService = new KubeletReconcilerService(etcdService, containerService, objectMapper, etcdProperties, nodeName);
    }

    @Test
    public void testSyncOnBoot() {
        kubeletReconcilerService.syncOnBoot();
        verify(containerService, times(1)).removeOrphans(anySet());
    }

    @Test
    public void testStartReconciliationLoop() {
        kubeletReconcilerService.startReconciliationLoop();
        ContainerSpec spec = new ContainerSpec("webserver", "nginx:latest", 8080, 80);
        etcdService.put(assignementPrefix, spec);
        verify(containerService, timeout(2000).times(1)).reconcile(spec);
        // TODO figure out why the delete watch event isn't propagated in this test environment
        //etcdService.delete(assignementPrefix + "webserver");
        //verify(containerService, timeout(2000).times(1)).stopAndRemove("webserver");
    }

}
