## spring boot kube light

A collection of apps that mimic some kubernetes behavior

### etcd server
Most of the functionality is centered around etcd, both services interact with it which differs
from actual kubernetes where only the control plane does.

### manager-api
Provides a REST API to which you can POST and GET docker image manifests that describe
the desired system state and actual state.

### kubelet-agent
Multiple agents can be created (in docker compose).
Agents watch etcd and responds to changes made by the manager-api (or anywhere else),
creating and destroying docker containers as per the manifests.

Bring it all up with:\
`./startup.sh`

Interact with the manger-api with:
```shell
# add an nginx container 
curl -X POST \                       
-H "Content-Type: application/json" \
-d '{
  "value": {
    "name": "web-server",
    "image": "nginx:latest",
    "hostPort": 8081,
    "containerPort": 80
  }
}' \
http://localhost:9220/put

# get docker manifest for given key, this assumes node-1 was assigned the task of running web-server, change node name as appropriate
curl http://localhost:9220/get?key=/registry/containers/specs/node-1/web-server

# delete docker manifest for given key, as above, this assumes node-1 was responsible for web-server
curl http://localhost:9220/delete?key=/registry/containers/specs/node-1/web-server

# get desired container specification and actual state, similar to kubectl get pods
curl http://localhost:9220/api/containers

# get node heartbeat status
curl http://localhost:9220/api/nodes
```