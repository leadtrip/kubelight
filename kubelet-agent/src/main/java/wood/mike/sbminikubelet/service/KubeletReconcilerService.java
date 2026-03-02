package wood.mike.sbminikubelet.service;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import wood.mike.model.ContainerSpec;

import java.util.HashSet;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
@Service
public class KubeletReconcilerService implements CommandLineRunner {

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

    @Override
    public void run(String... args) {
        syncOnBoot();
        startReconciliationLoop();
    }

    public void syncOnBoot() {
        log.info("Starting initial sync from etcd...");
        try {
            GetResponse response = client.getKVClient()
                    .get(ByteSequence.from(watchDirectory, UTF_8),
                            GetOption.builder().isPrefix(true).build())
                    .get();

            Set<String> desiredContainerNames = new HashSet<>();

            for (KeyValue kv : response.getKvs()) {
                ContainerSpec spec = objectMapper.readValue(kv.getValue().getBytes(), ContainerSpec.class);
                desiredContainerNames.add(spec.name());
                containerService.reconcile(spec);
            }

            containerService.removeOrphans(desiredContainerNames);

        } catch (Exception e) {
            log.error("Initial sync failed!", e);
        }
    }

    public void startReconciliationLoop() {
        client.getWatchClient().watch(
            ByteSequence.from(watchDirectory, UTF_8),
            WatchOption.builder().isPrefix(true).build(),
            response -> {
                for (WatchEvent event : response.getEvents()) {
                    handleWatchEvent(event);
                }
            }
        );
    }

    private void handleWatchEvent(WatchEvent event) {
        String key = event.getKeyValue().getKey().toString(UTF_8);

        if (event.getEventType() == WatchEvent.EventType.PUT) {
            byte[] val = event.getKeyValue().getValue().getBytes();
            ContainerSpec spec = objectMapper.readValue(val, ContainerSpec.class);
            containerService.reconcile(spec);
        }
        else if (event.getEventType() == WatchEvent.EventType.DELETE) {
            String containerName = key.substring(key.lastIndexOf("/") + 1);
            log.info("Delete event detected for: {}", containerName);
            containerService.stopAndRemove(containerName);
        }
    }
}
