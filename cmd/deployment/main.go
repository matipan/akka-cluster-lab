package main

import (
	"context"
	"errors"
	"flag"
	"fmt"
	"log"
	"net/http"
	"os"
	"os/exec"
	"os/signal"
	"sort"
	"syscall"
	"time"

	"go.uber.org/ratelimit"
)

var akkaCluster = flag.String("host", "http://localhost:8558", "akka cluster management host")
var url = flag.String("u", "http://localhost:8080", "orders server URL")
var d = flag.Duration("d", 1*time.Minute, "time to run each load test")
var q = flag.Int("q", 1, "QPS for each create and get operation")
var test = flag.Int("t", 0, "test number. 0 to run all. To see all available tests use -h")
var h = flag.Bool("h", false, "show all tests")

const (
	orderMax = 100000
	userMax  = 1000
)

func checkErr(err error, info string) {
	if err != nil {
		log.Fatalf("%s: %s", info, err)
	}
}

func main() {
	flag.Parse()

	_, err := exec.LookPath("kubectl")
	if err != nil {
		log.Fatal("'kubectl' not found in PATH")
	}

	if *h {
		fmt.Printf(`Rolling deployment scenarios. These tests send POST and GET requests to the service while simultaneously
deploying a new version of the service. The test will run for %s and will send %d requests per second. The test options are:
0. Run all tests
1. Pod deletion for a single leader re-election (best scenario),
2. 3 leader reelections,
3. 5 leader reelections,
4. 7 leader reelections

For example, to run the best scenario test: go run cmd/deployment -q 15 -d 2m -t 1`, *d, *q)
		return
	}

	ch := make(chan os.Signal, 1)
	signal.Notify(ch, os.Interrupt, syscall.SIGTERM)
	ctx, cancel := context.WithCancel(context.Background())

	go func() {
		<-ch
		cancel()
	}()

	fmt.Println("Restarting the cluster before the test...")
	checkErr(restartDeployment(), "restarting deployment failed")
	checkErr(watchDeployment(), "watch deployment failed")
	fmt.Println("Restart is done")

	if *test != 0 {
		checkErr(runTest(ctx, reelections(*test)), "test failed to run")
		return
	}

	msg := "Rolling deployment scenario. This test will send POST and GET requests to the service while simultaneously deploying a new version of the service. The test will run for %s and will send %d requests per second. The order will be as follows:\n1. Pod deletion for a single leader re-election (best scenario),\n2. 3 leader reelections,\n3. 5 leader reelections,\n4. 7 leader reelections\n"
	fmt.Printf(msg, *d, *q)

	checkErr(runTest(ctx, reelections(1)), "test 1 failed to run")
	restartCluster()
	checkErr(runTest(ctx, reelections(2)), "test 2 failed to run")
	restartCluster()
	checkErr(runTest(ctx, reelections(3)), "test 3 failed to run")
	restartCluster()
	checkErr(runTest(ctx, reelections(4)), "test 4 failed to run")
}

func restartCluster() {
	fmt.Println("Restarting the cluster before the test, this can take a bit of time...")
	checkErr(restartDeployment(), "restarting deployment failed")
	checkErr(watchDeployment(), "watch deployment failed")
	fmt.Println("Restart is done")
}

func reelections(test int) int {
	switch test {
	case 1:
		return 1
	case 2:
		return 3
	case 3:
		return 5
	case 4:
		return 7
	default:
		return -1
	}
}

func runTest(ctx context.Context, reelections int) error {
	if reelections < 1 {
		return errors.New("invalid reelections value, must be greater than 1")
	}

	fmt.Printf("Press Enter to start test with %d reelections\n", reelections)
	fmt.Scanln()

	pods, err := getPods()
	if err != nil {
		return err
	}
	sort.Sort(pods)

	leader, err := getLeaderIP(*akkaCluster)
	if err != nil {
		return err
	}

	if reelections == 1 {
		var pod Pod
		for _, p := range pods {
			if p.Status.PodIP == leader {
				pod = p
				break
			}
		}
		setPodDeletionCost(pod.Metadata.Name, "100000")
	} else {
		setDeploymentScenario(pods, reelections)
	}

	createLimit := ratelimit.New(*q)
	getLimit := ratelimit.New(*q)
	c := &http.Client{
		Timeout: 1 * time.Second,
	}

	go func(ctx context.Context) {
		delay := time.After(10 * time.Second)
		select {
		case <-ctx.Done():
			return
		case <-delay:
			fmt.Println("10 seconds expire, deploying the service")
			checkErr(restartDeployment(), "restarting deployment failed")
			checkErr(watchDeployment(), "watch deployment failed")
			fmt.Println("deployment is done")
		}
	}(ctx)

	result := sendTraffic(ctx, c, createLimit, getLimit, *d)

	fmt.Printf("Test with %d reelections finished. Results:\n", reelections)
	result.printReport()

	return nil
}
