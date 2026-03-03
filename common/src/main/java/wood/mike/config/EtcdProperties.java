package wood.mike.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.etcd")
public record EtcdProperties(
        String endpoints,
        String containerPrefix,
        String containerStatusPrefix,
        String nodeStatusPrefix,
        Long timeoutSeconds,
        Long leaseTtlSeconds
) {}