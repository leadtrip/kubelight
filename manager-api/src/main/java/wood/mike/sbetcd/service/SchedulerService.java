package wood.mike.sbetcd.service;

import org.springframework.stereotype.Service;
import wood.mike.config.EtcdProperties;
import wood.mike.model.ContainerSpec;
import wood.mike.model.NodeStatus;
import wood.mike.service.EtcdService;

import java.util.List;

@Service
public class SchedulerService {

    private final EtcdService etcdService;
    private final EtcdProperties props;

    public SchedulerService(EtcdService etcdService, EtcdProperties props) {
        this.etcdService = etcdService;
        this.props = props;
    }

    public void scheduleContainer(ContainerSpec spec) {
        List<NodeStatus> nodes = etcdService.listPrefix(props.nodeStatusPrefix(), NodeStatus.class);

        // TODO this is the most basic method of selecting a node to run, to refine
        NodeStatus selectedNode = nodes.get(0);

        String destinationKey = props.containerPrefix() + selectedNode.nodeName() + "/" + spec.name();
        etcdService.put(destinationKey, spec);
    }
}
