package wood.mike.sbminikubelet.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import wood.mike.model.ContainerSpec;

@Service
public class ContainerService {

    private final DockerClient dockerClient;

    public ContainerService(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    @SneakyThrows
    public String deployContainer(final ContainerSpec containerSpec) {
        dockerClient.pullImageCmd(containerSpec.image())
                .start()
                .awaitCompletion();

        CreateContainerResponse container = dockerClient.createContainerCmd(containerSpec.image())
                .withName(containerSpec.name())
                .withHostConfig(HostConfig.newHostConfig().withPortBindings()
                        .withPortBindings(
                                new PortBinding(Ports.Binding.bindPort(containerSpec.hostPort()), ExposedPort.tcp(containerSpec.containerPort())))
                )
                .exec();

        dockerClient.startContainerCmd(container.getId()).exec();

        return container.getId();
    }
}
