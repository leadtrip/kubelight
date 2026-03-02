package wood.mike.sbminikubelet;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import wood.mike.sbminikubelet.service.KubeletReconcilerService;

@Slf4j
@SpringBootApplication(scanBasePackages = {"wood.mike"})
public class SbMiniKubeletApplication implements CommandLineRunner {

    @Autowired
    private KubeletReconcilerService kubeletReconcilerService;

    public static void main(String[] args) throws InterruptedException {
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(SbMiniKubeletApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);

        log.info("Kubelet is now anchored and waiting for events...");

        Thread.currentThread().join();
    }

    @Override
    public void run(String... args) {
        kubeletReconcilerService.startReconciliationLoop();
    }
}
