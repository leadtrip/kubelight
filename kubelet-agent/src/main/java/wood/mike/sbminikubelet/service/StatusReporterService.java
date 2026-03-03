package wood.mike.sbminikubelet.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import wood.mike.model.ContainerStatus;
import wood.mike.sbminikubelet.config.AppConstants;
import wood.mike.service.EtcdService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class StatusReporterService {

    private final DockerClient dockerClient;
    private final EtcdService etcdService;

    public StatusReporterService(DockerClient dockerClient, EtcdService etcdService) {
        this.dockerClient = dockerClient;
        this.etcdService = etcdService;
    }

    @Scheduled(fixedRate = 10000)
    public void reportStatus() {
        log.debug("Reporting container status to etcd...");

        List<Container> managedContainers = dockerClient.listContainersCmd()
                .withShowAll(true)
                .withLabelFilter(Map.of(AppConstants.MANAGED_BY_KEY, AppConstants.MANAGED_BY_VALUE))
                .exec();

        for (Container container : managedContainers) {
            String name = container.getNames()[0].replace("/", "");

            ContainerStatus status = new ContainerStatus(
                    name,
                    container.getState(),
                    container.getImageId(),
                    container.getStatus(),
                    LocalDateTime.now().toString()
            );

            String statusKey = "/registry/status/" + name;
            etcdService.put(statusKey, status);
        }
    }
}