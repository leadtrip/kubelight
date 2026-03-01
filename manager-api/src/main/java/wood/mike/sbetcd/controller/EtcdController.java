package wood.mike.sbetcd.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import wood.mike.sbetcd.model.*;
import wood.mike.sbetcd.service.EtcdService;

@Slf4j
@RestController
public class EtcdController {

    private final EtcdService etcdService;

    public EtcdController(EtcdService etcdService) {
        this.etcdService = etcdService;
    }

    @PostMapping("/put")
    public PutResponse put(@RequestBody PutRequest putRequest) {
        log.info("PutRequest: {}", putRequest);
        etcdService.put(putRequest.key(), putRequest.value());
        return PutResponse.success();
    }

    @GetMapping("/get/{key}")
    public GetResponse get(@PathVariable String key) {
        log.info("GetRequest: {}", key);
        String value = etcdService.get(key);
        return GetResponse.success(value);
    }

    @GetMapping("/watch/{key}")
    public WatchResponse watch(@PathVariable String key) {
        log.info("WatchRequest: {}", key);
        boolean keyExists = etcdService.watch(key);
        return WatchResponse.success(keyExists);
    }

    @GetMapping("/delete/{key}")
    public DeleteResponse delete(@PathVariable String key) {
        log.info("DeleteRequest: {}", key);
        etcdService.delete(key);
        return DeleteResponse.success();
    }
}
