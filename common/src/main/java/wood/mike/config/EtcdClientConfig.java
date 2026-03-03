package wood.mike.config;

import io.etcd.jetcd.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class EtcdClientConfig {

    @Bean(destroyMethod = "close")
    public Client etcdClient(@Value("${app.etcd.endpoints}") String endpoints) {
        return Client.builder()
                .endpoints(endpoints)
                .build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
