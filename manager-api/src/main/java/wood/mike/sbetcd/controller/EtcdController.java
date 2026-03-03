package wood.mike.sbetcd.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import wood.mike.model.ContainerSpec;
import wood.mike.model.ContainerStatus;
import wood.mike.model.FullContainerInfo;
import wood.mike.sbetcd.model.*;
import wood.mike.service.EtcdService;

import java.util.List;

@Slf4j
@RestController
public class EtcdController {

    private final EtcdService etcdService;

    public EtcdController(EtcdService etcdService) {
        this.etcdService = etcdService;
    }

    @PostMapping("/api/put")
    public KlPutResponse put(@RequestBody KlPutRequest putRequest) {
        log.info("PutRequest: {}", putRequest);
        etcdService.put(putRequest.key(), putRequest.value());
        return KlPutResponse.success();
    }

    @GetMapping("/api/get")
    public KlGetResponse get(@RequestParam String key) {
        log.info("GetRequest: {}", key);
        ContainerSpec value = etcdService.getValue(key, ContainerSpec.class).orElse(null);
        return KlGetResponse.success(value);
    }

    @GetMapping("/api/delete")
    public KlDeleteResponse delete(@RequestParam String key) {
        log.info("DeleteRequest: {}", key);
        etcdService.delete(key);
        return KlDeleteResponse.success();
    }

    @GetMapping("/api/containers")
    public List<FullContainerInfo> getAllContainers() {
        List<ContainerSpec> specs = etcdService.listPrefix("/registry/containers/", ContainerSpec.class);

        return specs.stream().map(spec -> {
            ContainerStatus status = etcdService.getValue("/registry/status/" + spec.name(), ContainerStatus.class)
                    .orElse(new ContainerStatus(spec.name(), "Unknown", "N/A", "N/A", "N/A"));

            return new FullContainerInfo(spec, status);
        }).toList();
    }
}
