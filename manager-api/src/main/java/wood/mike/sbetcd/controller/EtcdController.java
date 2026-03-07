package wood.mike.sbetcd.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import wood.mike.config.EtcdProperties;
import wood.mike.model.*;
import wood.mike.sbetcd.model.*;
import wood.mike.sbetcd.service.SchedulerService;
import wood.mike.service.EtcdService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@Tag(name = "Container Management", description = "Endpoints for managing lifecycle of containers")
public class EtcdController {

    private final EtcdService etcdService;
    private final EtcdProperties etcdProperties;
    private final SchedulerService schedulerService;

    public EtcdController(EtcdService etcdService,
                          EtcdProperties etcdProperties,
                          SchedulerService schedulerService) {
        this.etcdService = etcdService;
        this.etcdProperties = etcdProperties;
        this.schedulerService = schedulerService;
    }

    @Operation(summary = "Submit a new container spec",
            description = "Persists a ContainerSpec to etcd for the kubelet to pick up")
    @PostMapping("/api/put")
    public KlPutResponse put(@Validated @RequestBody ContainerSpec spec) {
        log.info("PutRequest: {}", spec);
        schedulerService.scheduleContainer(spec);
        return KlPutResponse.success();
    }

    @Operation(summary = "Get the container spec",
            description = "Fetch the container spec from etcd for the given key")
    @GetMapping("/api/get")
    public KlGetResponse get(@RequestParam String key) {
        log.info("GetRequest: {}", key);
        ContainerSpec value = etcdService.getValue(key, ContainerSpec.class).orElse(null);
        return KlGetResponse.success(value);
    }

    @Operation(summary = "Get container specs per node",
            description = "Fetch all the  container specs for the given node or all nodes if no node supplied in the request")
    @GetMapping("/api/node/containers")
    public List<NodeContainerInfo> getAllContainersForNode(@RequestParam Optional<String> node) {
        return etcdService.listKeys(etcdProperties.containerPrefix() + node.orElse(""))
                .stream()
                .map(etcdService::extractNodeFromKey)
                .distinct()
                .map(n -> {
                    List<ContainerSpec> containerSpecs = etcdService.listPrefix(etcdProperties.containerPrefix() + n, ContainerSpec.class);
                    return new NodeContainerInfo(n, containerSpecs);
                })
                .collect(Collectors.toList());
    }

    @Operation(summary = "Delete a container",
            description = "Delete the a container spec for the given key")
    @GetMapping("/api/delete")
    public KlDeleteResponse delete(@RequestParam String key) {
        log.info("DeleteRequest: {}", key);
        etcdService.delete(key);
        return KlDeleteResponse.success();
    }

    @Operation(summary = "Get all container specs and status",
            description = "Fetch all container specs (the desired state) and the actual state for the given container")
    @GetMapping("/api/containers")
    public List<FullContainerInfo> getAllContainers() {
        List<ContainerSpec> specs = etcdService.listPrefix(etcdProperties.containerPrefix(), ContainerSpec.class);

        Map<String, ContainerStatus> statusMap = etcdService.listPrefix(etcdProperties.containerStatusPrefix(), ContainerStatus.class)
                .stream()
                .collect(Collectors.toMap(ContainerStatus::name, s -> s, (existing, replacement) -> replacement));

        return specs.stream().map(spec -> {
            ContainerStatus status = statusMap.getOrDefault(spec.name(),
                    new ContainerStatus(spec.name(), "Unknown", "N/A", "N/A", "N/A"));

            return new FullContainerInfo(spec, status);
        }).toList();
    }

    @Operation(summary = "Get node statuses",
            description = "Fetch the status of all nodes")
    @GetMapping("/api/nodes")
    public List<NodeStatus> getAllNodes() {
        return etcdService.listPrefix(etcdProperties.nodeStatusPrefix(), NodeStatus.class);
    }
}
