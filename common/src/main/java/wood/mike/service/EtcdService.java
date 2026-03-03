package wood.mike.service;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import wood.mike.exception.EtcdOperationException;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
@Service
public class EtcdService {

    private final Long timeout;
    private final Client client;
    private final ObjectMapper objectMapper;

    public EtcdService(
            Client client,
            @Value("${app.etcd.timeout:5}") Long timeout,
            ObjectMapper objectMapper
    ) {
        this.objectMapper = objectMapper;
        this.timeout = timeout;
        this.client = client;
    }

    public <T> void put(String key, T value) {
        try {
            byte[] data = objectMapper.writeValueAsBytes(value);

            client.getKVClient()
                    .put(ByteSequence.from(key, UTF_8), ByteSequence.from(data))
                    .get(timeout, TimeUnit.SECONDS);

            log.info("Successfully persisted key: {}", key);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new EtcdOperationException("Failed to put key: " + key, e);
        }
    }

    public <T> Optional<T> getValue(String key, Class<T> clazz) {
        try {
            var response = client.getKVClient()
                    .get(ByteSequence.from(key, UTF_8))
                    .get(timeout, TimeUnit.SECONDS);
            if (response.getKvs().isEmpty()) return Optional.empty();

            return Optional.of(objectMapper.readValue(response.getKvs().getFirst().getValue().getBytes(), clazz));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public GetResponse get(String key, boolean isPrefix) throws ExecutionException, InterruptedException {
        return client.getKVClient()
                .get(ByteSequence.from(key, UTF_8),
                        GetOption.builder().isPrefix(isPrefix).build())
                .get();
    }

    public void delete(String key) {
        try {
            client.getKVClient().delete(ByteSequence.from(key, UTF_8)).get(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new EtcdOperationException("Failed to delete key: " + key, e);
        }
    }

    public void watchPrefix(String prefix, Consumer<WatchResponse> eventHandler) {
        Watch.Listener listener = Watch.listener(eventHandler, error -> {
            log.error("Etcd watch error for prefix {}: {}", prefix, error.getMessage());
        });

        client.getWatchClient().watch(
                ByteSequence.from(prefix, UTF_8),
                WatchOption.builder().isPrefix(true).build(),
                listener
        );
    }

    public <T> List<T> listPrefix(String prefix, Class<T> clazz) {
        try {
            var response = client.getKVClient()
                    .get(
                            ByteSequence.from(prefix, UTF_8),
                            GetOption.builder().isPrefix(true).build()
                    )
                    .get(timeout, TimeUnit.SECONDS);

            return response.getKvs().stream()
                    .map(kv -> {
                        return objectMapper.readValue(kv.getValue().getBytes(), clazz);
                    })
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            log.error("Failed to list prefix: {}", prefix, e);
            return Collections.emptyList();
        }
    }
}