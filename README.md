## spring boot kube light

A collection of apps that mimic some kubernetes behavior

### manager-api
Provides a REST API to which you can POST and GET docker image manifests that describe
the desired system state. The docker manifests are stored in etcd.

### kubelet-agent
Watches etcd and responds to changes made by the manager-api (or anywhere else),
creating and destroying docker containers as per the manifests.

Bring it all up with:\
`./startup.sh`

Interact with the manger-api with:
```shell
# add an nginx container 
curl -X POST \                       
-H "Content-Type: application/json" \
-d '{
  "key": "/registry/containers/web-server",
  "value": {
    "name": "web-server",
    "image": "nginx:latest",
    "hostPort": 8081,
    "containerPort": 80
  }
}' \
http://localhost:9220/put

# get value for key
curl http://localhost:9220/get?key=/registry/containers/web-server

# delete key/value pair for given key
curl http://localhost:9220/delete?key=/registry/containers/web-server

# get desired container specification and actual state, similar to kubectl get pods
curl http://localhost:9220/api/containers
```