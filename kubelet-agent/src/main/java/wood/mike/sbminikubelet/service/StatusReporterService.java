package wood.mike.sbminikubelet.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import wood.mike.config.EtcdProperties;
import wood.mike.model.ContainerStatus;
import wood.mike.sbminikubelet.config.AppConstants;
import wood.mike.service.EtcdService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static wood.mike.sbminikubelet.config.AppConstants.*;

@Service
@Slf4j
public class StatusReporterService {

    private final DockerClient dockerClient;
    private final EtcdService etcdService;
    private final EtcdProperties etcdProperties;
    private final String nodeName;

    public StatusReporterService(DockerClient dockerClient,
                                 EtcdService etcdService,
                                 EtcdProperties etcdProperties,
                                 @Value("${app.node-name}") String nodeName) {
        this.dockerClient = dockerClient;
        this.etcdService = etcdService;
        this.etcdProperties = etcdProperties;
        this.nodeName = nodeName;
    }

    @Scheduled(fixedRate = 10000)
    public void reportStatus() {
        log.info("Reporting container status to etcd...");

        List<Container> allManaged = dockerClient.listContainersCmd()
                .withShowAll(true)
                .withLabelFilter(Map.of(MANAGED_BY_KEY, MANAGED_BY_VALUE))
                .exec();

        log.info("Found {} managed containers", allManaged.size());

        for (Container container : allManaged) {
            String ownerNode = container.getLabels().get(OWNER_NODE_KEY);

            if (nodeName.equals(ownerNode)) {
                String name = container.getNames()[0].replace("/", "");

                ContainerStatus status = new ContainerStatus(
                        name,
                        container.getState(),
                        container.getImageId(),
                        container.getStatus(),
                        LocalDateTime.now().toString()
                );

                String statusKey = etcdProperties.containerStatusPrefix() + nodeName + "/" + name;
                etcdService.put(statusKey, status);
            } else {
                log.trace("Skipping status report for container {} owned by {}",
                        container.getNames()[0], ownerNode);
            }
        }
    }
}