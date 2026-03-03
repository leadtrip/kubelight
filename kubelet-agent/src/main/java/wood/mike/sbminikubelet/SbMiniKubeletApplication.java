package wood.mike.sbminikubelet;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@EnableScheduling
@SpringBootApplication(scanBasePackages = {"wood.mike"})
public class SbMiniKubeletApplication {
    public static void main(String[] args) throws InterruptedException {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(SbMiniKubeletApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);

        log.info("Kubelet is now anchored and waiting for events...");

        Thread.currentThread().join();
    }
}
