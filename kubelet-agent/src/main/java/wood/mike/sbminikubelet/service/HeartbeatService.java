package wood.mike.sbminikubelet.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import wood.mike.config.EtcdProperties;
import wood.mike.model.NodeStatus;
import wood.mike.service.EtcdService;

@Slf4j
@Service
public class HeartbeatService implements CommandLineRunner {

    private final EtcdService etcdService;
    private final EtcdProperties props;
    private final String nodeName;

    public HeartbeatService(
            EtcdService etcdService,
            EtcdProperties props,
            @Value("${app.node-name}") String nodeName) {
        this.etcdService = etcdService;
        this.props = props;
        this.nodeName = nodeName;
    }

    @Override
    public void run(String... args) {
        log.info("Starting Heartbeat Service");
        String key = props.nodeStatusPrefix() + nodeName;
        NodeStatus status = new NodeStatus(nodeName, "Ready", System.currentTimeMillis(), "v1.0");
        etcdService.putWithLease(key, status, props.leaseTtlSeconds());
    }

}
