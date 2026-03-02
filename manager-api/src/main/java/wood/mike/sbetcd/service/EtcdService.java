package wood.mike.sbetcd.service;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import wood.mike.model.ContainerSpec;
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

    public void put(String key, ContainerSpec value) {
        try {
            String json = objectMapper.writeValueAsString(value);

            client.getKVClient()
                    .put(ByteSequence.from(key, UTF_8), ByteSequence.from(json, UTF_8))
                    .get(timeout, TimeUnit.SECONDS);

            log.info("Successfully persisted spec for: {}", key);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new EtcdOperationException("Failed to put key: " + key, e);
        }
    }

    public ContainerSpec get(String key) {
        try {
            var response = client.getKVClient()
                    .get(ByteSequence.from(key, UTF_8))
                    .get(timeout, TimeUnit.SECONDS);

            if (response.getKvs().isEmpty()) return null;
            return objectMapper.readValue(response.getKvs().getFirst().getValue().getBytes(), ContainerSpec.class);
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

}