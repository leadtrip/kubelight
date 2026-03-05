package wood.mike.common.unit.service;

import io.etcd.jetcd.Client;
import io.etcd.jetcd.test.EtcdClusterExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import tools.jackson.databind.ObjectMapper;
import wood.mike.config.EtcdProperties;
import wood.mike.model.ContainerSpec;
import wood.mike.model.ContainerStatus;
import wood.mike.model.NodeStatus;
import wood.mike.service.EtcdService;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class EtcdServiceTest {

    @RegisterExtension
    private static final EtcdClusterExtension cluster = EtcdClusterExtension.builder()
            .withNodes(1)
            .build();

    private EtcdService etcdService;
    private EtcdProperties etcdProperties;

    @BeforeEach
    void setUp() {
        etcdProperties = new EtcdProperties(
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
    }

    @Test
    public void testAll() {
        ContainerSpec nginx = new ContainerSpec("web-server", "nginx:latest", 8081, 80);
        final String key = "nginx";
        etcdService.put(key, nginx);
        ContainerSpec containerSpec = etcdService.getValue(key, ContainerSpec.class).orElseThrow();
        assertEquals(nginx, containerSpec);
        etcdService.delete(key);
        assertTrue(etcdService.getValue(key, ContainerSpec.class).isEmpty());
    }

    @Test
    public void testExtractNodeFromKey() {
        String key = "/registry/containers/specs/node-1/nginx";
        String node = etcdService.extractNodeFromKey(key);
        assertEquals("node-1", node);

        assertNull(etcdService.extractNodeFromKey("/no/node/key"));
    }

    @Test
    public void testListKeys() {
        ContainerSpec nginx = new ContainerSpec("web-server", "nginx:latest", 8081, 80);
        final String nginxKey = etcdProperties.containerPrefix() + "nginx";
        ContainerSpec whoami = new ContainerSpec("whoami", "traefik/whoami:latest", 8082, 80);
        final String whoAmIKey = etcdProperties.containerPrefix() + "whoami";
        etcdService.put(nginxKey, nginx);
        etcdService.put(whoAmIKey, whoami);
        List<String> keyList = etcdService.listKeys("/registry/containers/specs/");
        assertEquals(keyList, List.of(nginxKey, whoAmIKey));
    }

    @Test
    public void testListPrefix() {

        NodeStatus node1 = new NodeStatus("node-1", "Ready", System.currentTimeMillis(), "v1.0");
        etcdService.put(etcdProperties.nodeStatusPrefix() + "node-1", node1);
        NodeStatus node2 = new NodeStatus("node-2", "Ready", System.currentTimeMillis(), "v1.0");
        etcdService.put(etcdProperties.nodeStatusPrefix() + "node-2", node2);
        List<NodeStatus> nodeStatuses = etcdService.listPrefix(etcdProperties.nodeStatusPrefix(), NodeStatus.class);
        assertEquals(2, nodeStatuses.size());
        assertTrue(nodeStatuses.contains(node1));
        assertTrue(nodeStatuses.contains(node2));
    }

    @Test
    public void testWatchPrefix() throws InterruptedException {
        BlockingQueue<Boolean> watchQueue = new ArrayBlockingQueue<>(1);

        etcdService.watchPrefix(etcdProperties.containerStatusPrefix(), eventHandler -> {
            watchQueue.offer(true);
        });
        etcdService.put(etcdProperties.containerStatusPrefix() + "node-1", new ContainerStatus("nginx", "running", "123", "1 day", "2 minutes"));
        assertEquals(Boolean.TRUE, watchQueue.poll(2, TimeUnit.SECONDS));
    }
}
