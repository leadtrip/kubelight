package wood.mike.sbminikubelet.service;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
@Service
public class KubeletReconcilerService implements CommandLineRunner {

    private final Client client;
    private final ContainerService dockerService;
    private final String watchDirectory;

    public KubeletReconcilerService(
            @Value("${app.etcd.endpoints}") String endpoints,
            @Value("${app.etcd.watch-directory}") String watchDirectory,
            ContainerService dockerService) {
        log.info("Initializing client, endpoints: {}", endpoints);
        this.dockerService = dockerService;
        this.watchDirectory = watchDirectory;
        this.client = Client.builder()
                .endpoints(endpoints)
                .build();
    }

    @Override
    public void run(String... args) throws Exception {
        startReconciliationLoop();
    }

    public void startReconciliationLoop() {
        client.getWatchClient().watch(
                ByteSequence.from(watchDirectory, UTF_8),
                WatchOption.builder().isPrefix(true).build(),
                response -> {
                    for (WatchEvent event : response.getEvents()) {
                        // 1. Get the Key and the JSON Value
                        log.info(event.toString());
                        // 2. Map JSON to ContainerSpec.class
                        // 3. Call Docker API to Create/Delete
                    }
                }
        );
    }
}
