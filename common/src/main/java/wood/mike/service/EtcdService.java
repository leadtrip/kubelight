package wood.mike.service;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.lease.LeaseKeepAliveResponse;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchResponse;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import wood.mike.config.EtcdProperties;
import wood.mike.exception.EtcdOperationException;

import java.nio.charset.StandardCharsets;
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

    private final Client client;
    private final ObjectMapper objectMapper;
    private final EtcdProperties etcdProperties;

    public EtcdService(
            Client client,
            ObjectMapper objectMapper,
            EtcdProperties etcdProperties
    ) {
        this.objectMapper = objectMapper;
        this.client = client;
        this.etcdProperties = etcdProperties;
    }

    public <T> void put(String key, T value) {
        try {
            byte[] data = objectMapper.writeValueAsBytes(value);

            client.getKVClient()
                    .put(ByteSequence.from(key, UTF_8), ByteSequence.from(data))
                    .get(etcdProperties.timeoutSeconds(), TimeUnit.SECONDS);

            log.info("Successfully persisted key: {}", key);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new EtcdOperationException("Failed to put key: " + key, e);
        }
    }

    /**
     * Add an entry with a lease of the given number of seconds.
     * Etcd will receive periodic pings ensuring it keeps the entry. After each ping a countdown
     * starts of the number of lease seconds, if this hits zero (basically the app is dead) the entry is removed by etcd
     * @param key           - the key
     * @param value         - the value
     * @param ttlSeconds    - the number of seconds to keep the entry in etcd after a ping from the app
     */
    public void putWithLease(String key, Object value, long ttlSeconds) {
        try {
            long leaseId = client.getLeaseClient().grant(ttlSeconds).get().getID();

            log.info("Putting key {} with lease {}", key, ttlSeconds);

            client.getLeaseClient().keepAlive(leaseId, new StreamObserver<>() {
                @Override
                public void onNext(LeaseKeepAliveResponse r) {
                }

                @Override
                public void onError(Throwable t) {
                }

                @Override
                public void onCompleted() {
                }
            });

            String json = objectMapper.writeValueAsString(value);
            log.info("Persisting with lease {}", json);
            client.getKVClient().put(
                    ByteSequence.from(key, StandardCharsets.UTF_8),
                    ByteSequence.from(json, StandardCharsets.UTF_8),
                    PutOption.builder().withLeaseId(leaseId).build()
            ).get();

        } catch (Exception e) {
            throw new RuntimeException("Failed to put with lease", e);
        }
    }

    public <T> Optional<T> getValue(String key, Class<T> clazz) {
        try {
            var response = client.getKVClient()
                    .get(ByteSequence.from(key, UTF_8))
                    .get(etcdProperties.timeoutSeconds(), TimeUnit.SECONDS);
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
            client.getKVClient().delete(ByteSequence.from(key, UTF_8)).get(etcdProperties.timeoutSeconds(), TimeUnit.SECONDS);
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
                    .get(etcdProperties.timeoutSeconds(), TimeUnit.SECONDS);

            return response.getKvs().stream()
                    .map(kv -> objectMapper.readValue(kv.getValue().getBytes(), clazz))
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            log.error("Failed to list prefix: {}", prefix, e);
            return Collections.emptyList();
        }
    }

    public List<String> listKeys(String prefix) {
        try {
            var response = client.getKVClient()
                    .get(
                            ByteSequence.from(prefix, UTF_8),
                            GetOption.builder().isPrefix(true).build()
                    )
                    .get(etcdProperties.timeoutSeconds(), TimeUnit.SECONDS);

            return response.getKvs().stream()
                    .map(kv -> kv.getKey().toString())
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            log.error("Failed to list keys for prefix: {}", prefix, e);
            return Collections.emptyList();
        }
    }

    public String extractNodeFromKey(String key) {
        String prefix = etcdProperties.containerPrefix();
        if (!key.startsWith(prefix)) return null;

        return key.substring(
                prefix.length(),
                key.lastIndexOf('/')
        );
    }
}