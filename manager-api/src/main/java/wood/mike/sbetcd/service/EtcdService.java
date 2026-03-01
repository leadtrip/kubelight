package wood.mike.sbetcd.service;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.options.WatchOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import wood.mike.sbetcd.exception.EtcdOperationException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
@Service
public class EtcdService {

    private final Long timeout;

    private final Client client;

    public EtcdService(
            @Value("${app.etcd.endpoints}") String endpoints,
            @Value("${app.etcd.timeout:5}") Long timeout
    ) {
        log.info("Etcd service initializing client, endpoints: {}, timeout: {}", endpoints, timeout);
        this.timeout = timeout;
        this.client = Client.builder()
                .endpoints(endpoints)
                .build();
    }

    public void put(String key, String value) {
        try {
            client.getKVClient()
                    .put(ByteSequence.from(key, UTF_8), ByteSequence.from(value, UTF_8))
                    .get(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new EtcdOperationException("Failed to put key: " + key, e);
        }
    }

    public String get(String key) {
        try {
            var response = client.getKVClient()
                    .get(ByteSequence.from(key, UTF_8))
                    .get(timeout, TimeUnit.SECONDS);

            if (response.getKvs().isEmpty()) return null;
            return response.getKvs().getFirst().getValue().toString(UTF_8);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new EtcdOperationException("Failed to get key: " + key, e);
        }
    }

    public void delete(String key) {
        try {
            client.getKVClient().delete(ByteSequence.from(key, UTF_8)).get(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new EtcdOperationException("Failed to delete key: " + key, e);
        }
    }

    public boolean watch(String  key) {
        client.getWatchClient().watch(ByteSequence.from(key, UTF_8),
                WatchOption.builder().withPrevKV(true).build(),
                response -> {
                    response.getEvents().forEach(event -> {
                        log.info("Key: {} changed from: {}, to: {} after: {} event",
                                event.getKeyValue().getKey(), event.getPrevKV().getValue(), event.getKeyValue().getValue(), event.getEventType());
                    });
                });
        return get(key) != null;
    }
}