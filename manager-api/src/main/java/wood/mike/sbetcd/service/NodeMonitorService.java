package wood.mike.sbetcd.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import wood.mike.config.EtcdProperties;
import wood.mike.model.ContainerSpec;
import wood.mike.model.NodeStatus;
import wood.mike.service.EtcdService;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NodeMonitorService {

    private final EtcdService etcdService;
    private final EtcdProperties etcdProperties;
    private final SchedulerService schedulerService;

    public NodeMonitorService(EtcdService etcdService,
                              EtcdProperties etcdProperties,
                              SchedulerService schedulerService) {
        this.etcdService = etcdService;
        this.etcdProperties = etcdProperties;
        this.schedulerService = schedulerService;
    }

    @Scheduled(fixedRate = 30000)
    public void rescueOrphanedContainers() {
        Set<String> activeNodes = etcdService.listPrefix(etcdProperties.nodeStatusPrefix(), NodeStatus.class)
                .stream()
                .map(NodeStatus::nodeName)
                .collect(Collectors.toSet());

        log.info("Active nodes: {}", activeNodes);

        List<String> allAssignmentKeys = etcdService.listKeys(etcdProperties.containerPrefix());

        for (String key : allAssignmentKeys) {
            String assignedNode = extractNodeFromKey(key);

            if (!activeNodes.contains(assignedNode)) {
                log.warn("Node {} is DEAD. Rescuing container from key: {}", assignedNode, key);

                Optional<ContainerSpec> spec = etcdService.getValue(key, ContainerSpec.class);
                spec.ifPresent(schedulerService::scheduleContainer);
                etcdService.delete(key);
            }
        }
    }

    public String extractNodeFromKey(String key) {
        String prefix = etcdProperties.containerPrefix();
        if (!key.startsWith(prefix)) return null;

        return key.substring(
                prefix.length(),
                key.lastIndexOf('/')
        );
    }
}
