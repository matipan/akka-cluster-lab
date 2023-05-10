# Toy project for Akka Sharding

This is a toy project that we are using to understand how Akka Cluster / Sharding works in practice. The application is a simple 
REST API to operate on Orders. Each Order is defined as an actor that gets sharded across the cluster.

TODO:
* [ ] **Akka persistence**: orders are stored in memory across the cluster but are lost on restarts. Using persistence with a DB like Mongo or Cassandra is the way to go according to akka's doc. This will allow us to test a wide range of new chaos scenarios.
* [ ] **[Chaos] Kill leaders and send traffic**: if we have a cluster of N instances and we are constantly: i) killing the leader every time it gets elected; ii) continously send traffic to create and fetch orders; should allow us to understand what is the impact of having a cluster under very heavy load that causes the leader to get continously reelected. It is important to track the error rate for requests separated into two: i) accessing existing actors on nodes that know the route to that actor; ii) accessing actors on a node that the route is unknown. With this two error rates we can understand the impact of having a ShardCoordinator that is unable to respond to "GetShardHome" messages [line 774 of ShardCoordinator.scala].
* [ ] **[Chaos] Deployment with and without PodDeletionCost**: understand the impact of deploying an Akka application where the pods are chosen at random. The benefit of having a PodDeletionCost defined should be clear. The theory is that downtimes will still occur but should be minimal since there will always be a single leader reelection.
* [ ] **[Chaos] Unstable persistence store**: if the database used for persisting events or a durable state gets unstable, what is the impact on the cluster? Once the database gets stable, is the cluster able to continue on like nothing happened or does it require manual intervention?
* [ ] **[Chaos] Added latency between nodes**: if we introduce network latency using `tc` between pods, how do systems respond? How long does it take to detect issues? How customizable is it?

## Deploying
To set this up locally you have to have `kind` and `kubectl` installed. Once that is taken care of, you can run the following commands:

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

# once pods are running, test it by creating an order
$ curl -X POST -v 'http://localhost:8080/orders?id=123&items=4&price=10.5&userID=4'
```
