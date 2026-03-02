package wood.mike.config;

import io.etcd.jetcd.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EtcdClientConfig {

    @Bean(destroyMethod = "close")
    public Client etcdClient(@Value("${app.etcd.endpoints}") String endpoints) {
        return Client.builder()
                .endpoints(endpoints)
                .build();
    }
}
