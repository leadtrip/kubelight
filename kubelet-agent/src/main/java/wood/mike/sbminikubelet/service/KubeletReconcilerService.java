package wood.mike.sbminikubelet.service;

import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.watch.WatchEvent;
import io.etcd.jetcd.watch.WatchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import wood.mike.model.ContainerSpec;
import wood.mike.service.EtcdService;

import java.util.HashSet;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
@Service
public class KubeletReconcilerService {

    private final EtcdService etcdService;
    private final ContainerService containerService;
    private final String watchDirectory;
    private final ObjectMapper objectMapper;

    public KubeletReconcilerService(
            EtcdService etcdService,
            @Value("${app.etcd.watch-directory}") String watchDirectory,
            ContainerService containerService,
            ObjectMapper objectMapper) {
        this.etcdService = etcdService;
        this.containerService = containerService;
        this.watchDirectory = watchDirectory;
        this.objectMapper = objectMapper;
    }

    public void start() {
        log.info("Starting Kubelet initialization...");
        syncOnBoot();
        startReconciliationLoop();
    }

    public void syncOnBoot() {
        log.info("Starting initial sync from etcd...");
        try {
            GetResponse response = etcdService.get(watchDirectory, true);

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
        log.info("Starting watch on {}", watchDirectory);
        etcdService.watchPrefix(watchDirectory, this::processResponse);
    }

    private void processResponse(WatchResponse response) {
        for (WatchEvent event : response.getEvents()) {
            try {
                handleWatchEvent(event);
            } catch (Exception e) {
                log.error("Error processing etcd event", e);
            }
        }
    }

    private void handleWatchEvent(WatchEvent event) {
        String key = event.getKeyValue().getKey().toString(UTF_8);

        switch (event.getEventType()) {
            case PUT -> {
                byte[] bytes = event.getKeyValue().getValue().getBytes();
                ContainerSpec spec = objectMapper.readValue(bytes, ContainerSpec.class);
                containerService.reconcile(spec);
            }
            case DELETE -> {
                String containerName = key.substring(key.lastIndexOf("/") + 1);
                log.info("Deleting container: {}", containerName);
                containerService.stopAndRemove(containerName);
            }
            case UNRECOGNIZED -> log.warn("Unknown etcd event type for key {}", key);
        }
    }
}
