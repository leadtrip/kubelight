package wood.mike.sbetcd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"wood.mike"})
public class SbEtcdApplication {

    public static void main(String[] args) {
        SpringApplication.run(SbEtcdApplication.class, args);
    }

}
