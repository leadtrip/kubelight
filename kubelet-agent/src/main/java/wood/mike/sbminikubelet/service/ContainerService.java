package wood.mike.sbminikubelet.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

@Service
public class ContainerService {

    private final DockerClient dockerClient;

    public ContainerService(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    @SneakyThrows
    public String deployContainer(String imageName, String containerName) {
        dockerClient.pullImageCmd(imageName)
                .start()
                .awaitCompletion();

        CreateContainerResponse container = dockerClient.createContainerCmd(imageName)
                .withName(containerName)
                .withHostConfig(HostConfig.newHostConfig()
                        .withPortBindings(PortBinding.parse("8081:80"))) // Map 8081 -> 80
                .exec();

        dockerClient.startContainerCmd(container.getId()).exec();

        return container.getId();
    }
}
