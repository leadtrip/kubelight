package wood.mike.sbetcd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"wood.mike"})
@EnableScheduling
public class SbEtcdApplication {

    public static void main(String[] args) {
        SpringApplication.run(SbEtcdApplication.class, args);
    }

}
