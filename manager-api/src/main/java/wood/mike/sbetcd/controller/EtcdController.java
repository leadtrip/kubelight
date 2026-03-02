package wood.mike.sbetcd.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import wood.mike.model.ContainerSpec;
import wood.mike.sbetcd.model.*;
import wood.mike.sbetcd.service.EtcdService;

@Slf4j
@RestController
public class EtcdController {

    private final EtcdService etcdService;

    public EtcdController(EtcdService etcdService) {
        this.etcdService = etcdService;
    }

    @PostMapping("/api/put")
    public PutResponse put(@RequestBody PutRequest putRequest) {
        log.info("PutRequest: {}", putRequest);
        etcdService.put(putRequest.key(), putRequest.value());
        return PutResponse.success();
    }

    @GetMapping("/api/get")
    public GetResponse get(@RequestParam String key) {
        log.info("GetRequest: {}", key);
        ContainerSpec value = etcdService.get(key);
        return GetResponse.success(value);
    }

    @GetMapping("/api/delete")
    public DeleteResponse delete(@RequestParam String key) {
        log.info("DeleteRequest: {}", key);
        etcdService.delete(key);
        return DeleteResponse.success();
    }
}
