package wood.mike.sbetcd.unit.service;

import io.etcd.jetcd.test.EtcdClusterExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import tools.jackson.databind.ObjectMapper;
import wood.mike.ContainerSpec;
import wood.mike.sbetcd.service.EtcdService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class EtcdServiceTest {

    @RegisterExtension
    private static final EtcdClusterExtension cluster = EtcdClusterExtension.builder()
            .withNodes(1)
            .build();

    private EtcdService etcdService;

    @BeforeEach
    void setUp() {
        String endpoint = cluster.clientEndpoints().get(0).toString();
        etcdService = new EtcdService(endpoint, 5L, new ObjectMapper());
    }

    @Test
    public void testAll() throws Exception {
        ContainerSpec nginx = new ContainerSpec("web-server", "nginx:latest", 8081, 80);
        final String key = "nginx";
        etcdService.put(key, nginx);
        assertEquals(nginx.name(), etcdService.get(key).name());
        assertEquals(nginx.image(), etcdService.get(key).image());
        assertEquals(nginx.hostPort(), etcdService.get(key).hostPort());
        assertEquals(nginx.containerPort(), etcdService.get(key).containerPort());
        etcdService.delete(key);
        assertNull(etcdService.get(key));
    }
}
