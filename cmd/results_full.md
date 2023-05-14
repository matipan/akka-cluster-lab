```sh
$ go run ./deployment/... -d 2m -q 20 -t 0
```
Restarting the cluster before the test...
Restart is done
Rolling deployment scenario. This test will send POST and GET requests to the service while simultaneously deploying a new version of the service. The test will run for 2m0s and will send 20 requests per second. The order will be as follows:
1. Pod deletion for a single leader re-election (best scenario),
2. 3 leader reelections,
3. 5 leader reelections,
4. 7 leader reelections
Press Enter to start test with 1 reelections

setting pod-deletion-cost=100000 to pod orders-5c9b5c76df-bn2kh
10 seconds expire, deploying the service
deployment is done
Test with 1 reelections finished. Results:
Status report for create methods:
{
  "0": 37,
  "200": 1639,
  "400": 12
}
        P99=36.098832ms - P95=27.689532ms - P50=11.422136ms
Status report for get methods:
{
  "0": 35,
  "200": 12,
  "404": 1628
}
        P99=31.256301ms - P95=21.606721ms - P50=10.587724ms
Total error rate = 2.14%

Restarting the cluster before the test, this can take a bit of time...
Restart is done
Press Enter to start test with 3 reelections

setting pod-deletion-cost=0 to pod orders-5475c4954b-6c8rm
setting pod-deletion-cost=0 to pod orders-5475c4954b-6c8rm
setting pod-deletion-cost=100 to pod orders-5475c4954b-qm7c5
setting pod-deletion-cost=100 to pod orders-5475c4954b-qm7c5
setting pod-deletion-cost=200 to pod orders-5475c4954b-46hf7
setting pod-deletion-cost=200 to pod orders-5475c4954b-46hf7
10 seconds expire, deploying the service
deployment is done
Test with 3 reelections finished. Results:
Status report for create methods:
{
  "0": 45,
  "200": 1432,
  "400": 9
}
        P99=1.001955247s - P95=1.00091855s - P50=14.425427ms
Status report for get methods:
{
  "0": 44,
  "200": 8,
  "404": 1528
}
        P99=1.003564116s - P95=1.001559269s - P50=10.917115ms
Total error rate = 2.90%

Restarting the cluster before the test, this can take a bit of time...
Restart is done
Press Enter to start test with 5 reelections

setting pod-deletion-cost=0 to pod orders-5cd847f58d-mkq52
setting pod-deletion-cost=0 to pod orders-5cd847f58d-mkq52
setting pod-deletion-cost=100 to pod orders-5cd847f58d-ttsqn
setting pod-deletion-cost=100 to pod orders-5cd847f58d-ttsqn
setting pod-deletion-cost=200 to pod orders-5cd847f58d-w5sq6
setting pod-deletion-cost=200 to pod orders-5cd847f58d-w5sq6
setting pod-deletion-cost=300 to pod orders-5cd847f58d-nstw5
setting pod-deletion-cost=300 to pod orders-5cd847f58d-nstw5
setting pod-deletion-cost=400 to pod orders-5cd847f58d-57vc9
setting pod-deletion-cost=400 to pod orders-5cd847f58d-57vc9
10 seconds expire, deploying the service
deployment is done
Test with 5 reelections finished. Results:
Status report for create methods:
{
  "0": 56,
  "200": 1242,
  "400": 8
}
        P99=37.453454ms - P95=27.929705ms - P50=9.009432ms
Status report for get methods:
{
  "0": 58,
  "200": 4,
  "404": 1245
}
        P99=32.991012ms - P95=22.594598ms - P50=8.183399ms
Total error rate = 4.36%

Restarting the cluster before the test, this can take a bit of time...
Restart is done
Press Enter to start test with 7 reelections

setting pod-deletion-cost=0 to pod orders-5c9669dd75-vgncl
setting pod-deletion-cost=0 to pod orders-5c9669dd75-vgncl
setting pod-deletion-cost=100 to pod orders-5c9669dd75-9vnmx
setting pod-deletion-cost=100 to pod orders-5c9669dd75-9vnmx
setting pod-deletion-cost=200 to pod orders-5c9669dd75-vrnr9
setting pod-deletion-cost=200 to pod orders-5c9669dd75-vrnr9
setting pod-deletion-cost=300 to pod orders-5c9669dd75-sfql9
setting pod-deletion-cost=300 to pod orders-5c9669dd75-sfql9
setting pod-deletion-cost=400 to pod orders-5c9669dd75-r2mkb
setting pod-deletion-cost=400 to pod orders-5c9669dd75-r2mkb
setting pod-deletion-cost=500 to pod orders-5c9669dd75-rshsw
setting pod-deletion-cost=500 to pod orders-5c9669dd75-rshsw
setting pod-deletion-cost=600 to pod orders-5c9669dd75-gsjmf
setting pod-deletion-cost=600 to pod orders-5c9669dd75-gsjmf
10 seconds expire, deploying the service
deployment is done
Test with 7 reelections finished. Results:
Status report for create methods:
{
  "0": 69,
  "200": 1014,
  "400": 10
}
        P99=31.221306ms - P95=26.724579ms - P50=10.631842ms
Status report for get methods:
{
  "0": 69,
  "200": 4,
  "404": 1025
}
        P99=34.606877ms - P95=24.048271ms - P50=8.703349ms
Total error rate = 6.30%
