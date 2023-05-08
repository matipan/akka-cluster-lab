# Toy project for Akka Sharding

This is a toy project that we are using to understand how Akka Cluster / Sharding works in practice. The application is a simple 
chat app that has a single Room actor and multiple Client actors that are sharded across the entire cluster.

## Deploying
The idea is to test this setup locally. To do that you have to have `kind` and `kubectl` installed locally. Once that is taken
care of, you can run the following commands:

```sh
# create the kind cluster
$ kind create cluster --config k8s/kind-cluster.yaml

# build an image for the service
$ docker build -t clusterchat:0.0.1 .

# load the image into the cluster
$ kind load docker-image clusterchat:0.0.1 -n cluster-chat

# create the namespace, deployment, service and roles in k8s
$ kubectl apply -f k8s/spec.yaml

# check that pods start running successfully
$ kubectl -n clusterchat get pods --watch

# once pods are running, test it with a join
$ curl -X POST -v 'http://localhost:32000/join?username=cakka'
```
