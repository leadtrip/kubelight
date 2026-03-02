package wood.mike.sbminikubelet.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import wood.mike.model.ContainerSpec;
import wood.mike.sbminikubelet.config.AppConstants;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class ContainerService {

    private final DockerClient dockerClient;

    public ContainerService(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    public void reconcile(ContainerSpec spec) {
        try {
            var container = dockerClient.inspectContainerCmd(spec.name()).exec();
            String currentImage = container.getConfig().getImage();
            Boolean isRunning = container.getState().getRunning();

            log.info("Container {} exists. Image: {}, Running: {}", spec.name(), currentImage, isRunning);

            if (!currentImage.equals(spec.image())) {
                log.info("Image mismatch! Desired: {}, Actual: {}. Restarting...", spec.image(), currentImage);
                updateContainer(spec);
                return;
            }

            if (Boolean.FALSE.equals(isRunning)) {
                log.info("Container {} is stopped. Starting...", spec.name());
                dockerClient.startContainerCmd(spec.name()).exec();
            }

        } catch (NotFoundException e) {
            log.info("Container {} not found. Creating new...", spec.name());
            createAndStart(spec);
        }
    }

    @SneakyThrows
    public void createAndStart(final ContainerSpec containerSpec) {
        dockerClient.pullImageCmd(containerSpec.image())
                .start()
                .awaitCompletion();

        CreateContainerResponse container = dockerClient.createContainerCmd(containerSpec.image())
                .withName(containerSpec.name())
                .withLabels(Map.of(AppConstants.MANAGED_BY_KEY, AppConstants.MANAGED_BY_VALUE))
                .withHostConfig(HostConfig.newHostConfig().withPortBindings()
                        .withPortBindings(
                                new PortBinding(
                                        Ports.Binding.bindPort(containerSpec.hostPort()),
                                        ExposedPort.tcp(containerSpec.containerPort())))
                )
                .exec();

        dockerClient.startContainerCmd(container.getId()).exec();
        log.info("Container id: {} started", container.getId());
    }

    public void stopAndRemove(String containerName) {
        var container = dockerClient.inspectContainerCmd(containerName).exec();
        dockerClient.stopContainerCmd(container.getId()).exec();
        dockerClient.removeContainerCmd(container.getId()).exec();
        log.info("Container: {} removed.", containerName);
    }

    private void updateContainer(ContainerSpec spec) {
        dockerClient.stopContainerCmd(spec.name()).exec();
        dockerClient.removeContainerCmd(spec.name()).exec();
        createAndStart(spec);
    }

    /**
     * Remove any containers that are labeled but aren't in the given list
     * @param desiredNames  - the list of container names that should be running
     */
    public void removeOrphans(Set<String> desiredNames) {
        List<Container> allContainers = dockerClient.listContainersCmd()
                .withShowAll(true)
                .exec();

        for (Container container : allContainers) {
            String name = container.getNames()[0].replace("/", "");
            String managedBy = container.getLabels().get(AppConstants.MANAGED_BY_KEY);

            if (AppConstants.MANAGED_BY_VALUE.equals(managedBy) && !desiredNames.contains(name)) {
                log.info("Removing orphan: {}", name);
                stopAndRemove(name);
            }
        }
    }
}
