package wood.mike.sbminikubelet.service;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import wood.mike.model.ContainerSpec;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
@Service
public class KubeletReconcilerService {

    private final Client client;
    private final ContainerService containerService;
    private final String watchDirectory;
    private final ObjectMapper objectMapper;

    public KubeletReconcilerService(
            Client client,
            @Value("${app.etcd.watch-directory}") String watchDirectory,
            ContainerService containerService,
            ObjectMapper objectMapper) {
        this.containerService = containerService;
        this.watchDirectory = watchDirectory;
        this.objectMapper = objectMapper;
        this.client = client;
    }

    public void startReconciliationLoop() {
        client.getWatchClient().watch(
                ByteSequence.from(watchDirectory, UTF_8),
                WatchOption.builder().isPrefix(true).build(),
                response -> {
                    for (WatchEvent event : response.getEvents()) {
                        log.info(event.toString());
                        ContainerSpec containerSpec = objectMapper.readValue(event.getKeyValue().getValue().getBytes(), ContainerSpec.class);
                        String containerId = containerService.deployContainer(containerSpec);
                        log.info("Successfully deployed: {} container with id: {}", containerSpec.name(), containerId);
                    }
                }
        );
    }
}
