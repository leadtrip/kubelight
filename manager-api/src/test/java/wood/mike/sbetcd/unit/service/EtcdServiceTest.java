package wood.mike.sbetcd.unit.service;

import io.etcd.jetcd.test.EtcdClusterExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
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
        etcdService = new EtcdService(endpoint, 5L);
    }

    @Test
    public void testAll() throws Exception {
        final String key = "phone";
        final String pixel = "pixel";
        final String iphone = "iphone";
        etcdService.put(key, pixel);
        assertEquals(pixel, etcdService.get(key));
        etcdService.put(key, iphone);
        assertEquals(iphone, etcdService.get(key));
        etcdService.delete(key);
        assertNull(etcdService.get(key));
    }
}
