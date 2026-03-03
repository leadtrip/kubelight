package wood.mike.common.unit.service;

import io.etcd.jetcd.Client;
import io.etcd.jetcd.test.EtcdClusterExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import tools.jackson.databind.ObjectMapper;
import wood.mike.config.EtcdProperties;
import wood.mike.model.ContainerSpec;
import wood.mike.service.EtcdService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class EtcdServiceTest {

    @RegisterExtension
    private static final EtcdClusterExtension cluster = EtcdClusterExtension.builder()
            .withNodes(1)
            .build();

    private EtcdService etcdService;

    @BeforeEach
    void setUp() {
        EtcdProperties etcdProperties = new EtcdProperties("", "", "", 2L);
        String endpoint = cluster.clientEndpoints().get(0).toString();
        Client client = Client.builder().endpoints(endpoint).build();
        etcdService = new EtcdService(client, new ObjectMapper(), etcdProperties);
    }

    @Test
    public void testAll() {
        ContainerSpec nginx = new ContainerSpec("web-server", "nginx:latest", 8081, 80);
        final String key = "nginx";
        etcdService.put(key, nginx);
        ContainerSpec containerSpec = etcdService.getValue(key, ContainerSpec.class).orElseThrow();
        assertEquals(nginx.name(), containerSpec.name());
        assertEquals(nginx.image(), containerSpec.image());
        assertEquals(nginx.hostPort(), containerSpec.hostPort());
        assertEquals(nginx.containerPort(), containerSpec.containerPort());
        etcdService.delete(key);
        assertTrue(etcdService.getValue(key, ContainerSpec.class).isEmpty());
    }
}
