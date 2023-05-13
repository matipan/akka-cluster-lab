#### Cluster=4 Replicas - Client=1 RPS for Create and 1 RPS for Get with a timeout of 1 second for both ####

<--------------------------------------------------------------------->
Baseline operation with no restarts

```sh
$ go run deployment/main.go -d 2m
time elapsed
Status report for create methods:
{
	"200": 121
}
P99=14.761515ms - P95=12.399795ms - P50=7.956406ms
Status report for get methods:
{
	"200": 61,
	"404": 61
}
P99=23.125107ms - P95=20.273237ms - P50=11.857985ms
```

**Question**: when I request an actor that does not exist, does the ShardRegion have to ask the shard coordiantor every time? Or is there some form of persistence? There shouldn't because that actor could later on be created


<--------------------------------------------------------------------->
Deployment 5 seconds after that test started:

```sh
$ go run deployment/main.go -d 2m
time elapsed
Status report for create methods:
{
	"200": 121
}
P99=25.162061ms - P95=11.891503ms - P50=8.878502ms
Status report for get methods:
{
	"0": 11,
	"200": 44,
	"404": 67
}
P99=1.004263475s - P95=1.00033015s - P50=13.864045ms
```

Here we can already see how much the latency increase. The `0` status indicates that 11 requests timed out during this operation. A high error rate: 11/122 = 9%

**NOTE**: after the deployment finished the cluster kept rebalancing shards for a few minutes:
```
[2023-05-13 15:22:51,298] [INFO] [akka.cluster.sharding.DDataShardCoordinator] [OrderService-akka.actor.default-dispatcher-5] [akka://OrderService@10.244.0.33:25520/system/sharding/OrderCoordinator/singleton/coordinator] - Order: Starting rebalance for shards [454]. Current shards rebalancing: []
[2023-05-13 15:23:01,319] [INFO] [akka.cluster.sharding.DDataShardCoordinator] [OrderService-akka.actor.default-dispatcher-5] [akka://OrderService@10.244.0.33:25520/system/sharding/OrderCoordinator/singleton/coordinator] - Order: Starting rebalance for shards [459]. Current shards rebalancing: []
[2023-05-13 15:23:11,338] [INFO] [akka.cluster.sharding.DDataShardCoordinator] [OrderService-akka.actor.default-dispatcher-12] [akka://OrderService@10.244.0.33:25520/system/sharding/OrderCoordinator/singleton/coordinator] - Order: Starting rebalance for shards [479]. Current shards rebalancing: []
[2023-05-13 15:23:21,358] [INFO] [akka.cluster.sharding.DDataShardCoordinator] [OrderService-akka.actor.default-dispatcher-23] [akka://OrderService@10.244.0.33:25520/system/sharding/OrderCoordinator/singleton/coordinator] - Order: Starting rebalance for shards [485]. Current shards rebalancing: []
[2023-05-13 15:23:31,377] [INFO] [akka.cluster.sharding.DDataShardCoordinator] [OrderService-akka.actor.default-dispatcher-18] [akka://OrderService@10.244.0.33:25520/system/sharding/OrderCoordinator/singleton/coordinator] - Order: Starting rebalance for shards [49]. Current shards rebalancing: []
[2023-05-13 15:23:41,398] [INFO] [akka.cluster.sharding.DDataShardCoordinator] [OrderService-akka.actor.default-dispatcher-18] [akka://OrderService@10.244.0.33:25520/system/sharding/OrderCoordinator/singleton/coordinator] - Order: Starting rebalance for shards [508]. Current shards rebalancing: []
[2023-05-13 15:23:51,418] [INFO] [akka.cluster.sharding.DDataShardCoordinator] [OrderService-akka.actor.default-dispatcher-23] [akka://OrderService@10.244.0.33:25520/system/sharding/OrderCoordinator/singleton/coordinator] - Order: Starting rebalance for shards [52]. Current shards rebalancing: []
```

Running the test again with state from the previous run and forcing a deployment 5 seconds after:
```sh
$ go run deployment/main.go -d 2m
time elapsed
Status report for create methods:
{
	"200": 121
}
P99=19.51463ms - P95=15.22118ms - P50=9.221953ms
Status report for get methods:
{
	"0": 26,
	"200": 34,
	"404": 62
}
P99=1.001080645s - P95=1.000868831s - P50=15.028348ms
```

Even more errors now, an error rate of 26/26+34+62 = 21%

**NOTE**: rebalancing happened as well as last time for a few minutes

**Question**: could having the cluster with existing state make things worse? Meaning that there were already a bunch of shards that have to be rebalanced on deployment?

**New test**: send a bunch of create operations for 2 minutes. Then, run the test again and deploy after 5s.
First run went ok:
```sh
$ go run deployment/main.go -d 2m
time elapsed
Status report for create methods:
{
	"200": 121
}
P99=11.229752ms - P95=10.12956ms - P50=8.032817ms
Status report for get methods:
{
	"200": 59,
	"404": 63
}
P99=20.121254ms - P95=17.006165ms - P50=11.784487ms
```

Second run with deployment after 5s:

```sh
$ go run deployment/main.go -d 2m
time elapsed
Status report for create methods:
{
	"200": 121
}
P99=23.500578ms - P95=12.435975ms - P50=8.612009ms
Status report for get methods:
{
	"0": 18,
	"200": 41,
	"404": 63
}
P99=1.001137777s - P95=1.000697305s - P50=12.995523ms
```

**NOTE**: It is interesting that there are not errors for the create operation

An error rate of 14%. Not big enough to consider that having shards already in memory makes it worse. **This needs to be re-tested when we have persistence.**

<--------------------------------------------------------------------->
First run with pod deletion cost

```sh
$ go run deployment/main.go -d 2m
time elapsed
Status report for create methods:
{
	"200": 121
}
P99=23.512604ms - P95=16.639872ms - P50=9.243931ms
Status report for get methods:
{
	"0": 1,
	"200": 49,
	"404": 72
}
P99=425.898497ms - P95=54.554552ms - P50=18.754358ms
```

First run shows a much lower error rate

Second run same thing, this time absolutely no errors:

```sh
$ go run deployment/main.go -d 2m
time elapsed
Status report for create methods:
{
	"200": 121
}
P99=27.246241ms - P95=22.018053ms - P50=8.879824ms
Status report for get methods:
{
	"200": 42,
	"404": 80
}
P99=100.673581ms - P95=37.339929ms - P50=13.951429ms
```

One last run removing the pod deletion cost and observing 3 leader reelections:
```sh
$ go run deployment/main.go -d 2m
time elapsed
Status report for create methods:
{
  "200": 121
}
        P99=28.005996ms - P95=17.705946ms - P50=8.957568ms
Status report for get methods:
{
  "0": 31,
  "200": 33,
  "404": 58
}
        P99=1.002389219s - P95=1.000929874s - P50=18.389861ms
```

#### Cluster=8 Replicas - Client=15 RPS for Create and 15 RPS for Get with a timeout of 1 second for both ####

<--------------------------------------------------------------------->
Three runs without pod deletion cost to give chance to different kind of leader reelection scenarios:

Error rate = 12% - **5 leader reelections**
```sh
$ go run deployment/main.go -d 2m -q 15
time elapsed
Status report for create methods:
{
  "200": 1801
}
        P99=14.586615ms - P95=9.01776ms - P50=6.022417ms
Status report for get methods:
{
  "0": 83,
  "200": 238,
  "404": 353
}
        P99=38.789282ms - P95=22.944344ms - P50=9.127287ms
```

Error rate = 3% - **2 leader reelections**
```sh
$ go run deployment/main.go -d 2m -q 15
time elapsed
Status report for create methods:
{
  "200": 1801
}
        P99=10.777773ms - P95=9.101657ms - P50=5.588843ms
Status report for get methods:
{
  "0": 39,
  "200": 464,
  "404": 845
}
        P99=25.716891ms - P95=17.226814ms - P50=9.592054ms
```

Error rate = 3% - **2 leader reelections**
```sh
$ go run deployment/main.go -d 2m -q 15
time elapsed
Status report for create methods:
{
  "200": 1801
}
        P99=6.379936ms - P95=5.82348ms - P50=4.321526ms
Status report for get methods:
{
  "0": 37,
  "200": 464,
  "404": 869
}
        P99=16.81503ms - P95=12.10617ms - P50=9.240631ms
```

#### Cluster=8 Replicas - Client=20 RPS for Create and 20 RPS for Get with a timeout of 1 second for both ####

<--------------------------------------------------------------------->
Test the extreme: leader gets killed 8 times. This is done by manually checking the age of each member and setting a pod deletion cost that increases in that same order. Meaning that every time a new leader gets reelected it will get killed. Anarchy.

Error rate = 8%
```sh
 go run deployment/main.go -d 2m -q 20
time elapsed
Status report for create methods:
{
  "200": 2401
}
        P99=17.940622ms - P95=11.266848ms - P50=4.838531ms
Status report for get methods:
{
  "0": 73,
  "200": 369,
  "404": 580
}
        P99=26.15921ms - P95=19.726612ms - P50=8.141508ms
```

The other extreme. Pod deletion cost how it should be set.

Error rate = 1%
```sh
$ go run deployment/main.go -d 2m -q 20
time elapsed
Status report for create methods:
{
  "200": 2401
}
        P99=17.971602ms - P95=11.463866ms - P50=4.229364ms
Status report for get methods:
{
  "0": 22,
  "200": 659,
  "404": 1313
}
        P99=25.576274ms - P95=17.739866ms - P50=8.265392ms
```
