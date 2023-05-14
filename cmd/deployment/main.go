package main

import (
	"context"
	"encoding/json"
	"flag"
	"fmt"
	"log"
	"math/rand"
	"net/http"
	"os"
	"os/signal"
	"sync"
	"syscall"
	"time"

	"go.uber.org/ratelimit"

	"github.com/jamiealquiza/tachymeter"
)

var url = flag.String("u", "http://localhost:8080", "orders server URL")
var d = flag.Duration("d", 1*time.Minute, "time to run the test, 1 minute by default")
var q = flag.Int("q", 1, "QPS for each create and get operation")

var (
	muCreate     sync.Mutex
	createStatus = map[int]int{}
	orders       = []int{}

	muGet     sync.Mutex
	getStatus = map[int]int{}

	tget    = tachymeter.New(&tachymeter.Config{Size: 200})
	tcreate = tachymeter.New(&tachymeter.Config{Size: 200})
)

const (
	orderMax = 100000
	userMax  = 1000
)

func main() {
	flag.Parse()

	ch := make(chan os.Signal, 1)
	signal.Notify(ch, os.Interrupt, syscall.SIGTERM)
	ctx, cancel := context.WithCancel(context.Background())

	time.AfterFunc(*d, func() {
		// when time expires we cancel
		fmt.Println("time elapsed")
		cancel()
	})

	go func() {
		<-ch
		cancel()
	}()

	var wg sync.WaitGroup
	wg.Add(2)
	createLimit := ratelimit.New(*q)
	getLimit := ratelimit.New(*q)
	c := &http.Client{
		Timeout: 1 * time.Second,
	}

	go func(ctx context.Context) {
		for {
			select {
			case <-ctx.Done():
				wg.Done()
				return
			default:
				o := newOrder()
				order := fmt.Sprintf("id=%d&items=%d&price=%f&userID=%d", o.ID, o.Items, o.Price, o.UserID)

				r, err := http.NewRequest(http.MethodPost, *url+"/orders?"+order, nil)
				if err != nil {
					log.Fatal(err)
				}

				createLimit.Take()
				start := time.Now()
				res, err := c.Do(r)
				tcreate.AddTime(time.Since(start))
				if err != nil {
					muCreate.Lock()
					createStatus[0]++
					muCreate.Unlock()
					continue
				}
				res.Body.Close()

				muCreate.Lock()
				createStatus[res.StatusCode]++
				orders = append(orders, o.ID)
				muCreate.Unlock()
			}
		}
	}(ctx)

	go func(ctx context.Context) {
		for {
			select {
			case <-ctx.Done():
				wg.Done()
				return
			default:
				// we first get an order that we don't know if it exists or not
				order := rand.Intn(orderMax)
				getOrder(order, getLimit, c)

				// now we get an order that we know exists
				randOrder := -1
				muCreate.Lock()
				if len(orders) > 0 {
					randOrder = orders[rand.Intn(len(orders))]
				}
				muCreate.Unlock()

				if randOrder != -1 {
					getOrder(order, getLimit, c)
				}
			}
		}
	}(ctx)

	// here we have to report the status we observed for the different codes
	wg.Wait()

	bc, err := json.MarshalIndent(createStatus, "", "  ")
	if err != nil {
		log.Fatal(err)
	}

	bg, err := json.MarshalIndent(getStatus, "", "  ")
	if err != nil {
		log.Fatal(err)
	}

	getMetrics := tget.Calc()
	createMetrics := tcreate.Calc()
	fmt.Printf("Status report for create methods:\n%s\n\tP99=%s - P95=%s - P50=%s\n", bc, createMetrics.Time.P99, createMetrics.Time.P95, createMetrics.Time.P50)
	fmt.Printf("Status report for get methods:\n%s\n\tP99=%s - P95=%s - P50=%s\n", bg, getMetrics.Time.P99, getMetrics.Time.P95, getMetrics.Time.P50)
}

func getOrder(orderID int, limit ratelimit.Limiter, c *http.Client) {
	r, err := http.NewRequest(http.MethodGet, fmt.Sprintf("%s/orders/%d", *url, orderID), nil)
	if err != nil {
		log.Fatal(err)
	}

	limit.Take()
	start := time.Now()
	res, err := c.Do(r)
	tget.AddTime(time.Since(start))
	if err != nil {
		muGet.Lock()
		getStatus[0]++
		muGet.Unlock()
		return
	}
	res.Body.Close()

	muGet.Lock()
	getStatus[res.StatusCode]++
	muGet.Unlock()
}

type Order struct {
	ID     int
	Items  int
	Price  float32
	UserID int
}

func newOrder() Order {
	return Order{
		ID:     rand.Intn(orderMax),
		Items:  4,
		Price:  10.0,
		UserID: rand.Intn(userMax),
	}
}
