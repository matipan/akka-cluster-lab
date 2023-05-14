package main

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"math/rand"
	"net/http"
	"sync"
	"time"

	"github.com/jamiealquiza/tachymeter"
	"go.uber.org/ratelimit"
)

type result struct {
	getStatus  map[int]int
	getMetrics *tachymeter.Metrics

	createStatus  map[int]int
	createMetrics *tachymeter.Metrics
}

func (r result) printReport() {
	bc, _ := json.MarshalIndent(r.createStatus, "", "  ")
	bg, _ := json.MarshalIndent(r.getStatus, "", "  ")

	sum := 0
	for _, val := range r.getStatus {
		sum += val
	}
	for _, val := range r.createStatus {
		sum += val
	}

	errs := r.getStatus[0] + r.createStatus[0]

	fmt.Printf("Status report for create methods:\n%s\n\tP99=%s - P95=%s - P50=%s\n", bc, r.createMetrics.Time.P99, r.createMetrics.Time.P95, r.createMetrics.Time.P50)
	fmt.Printf("Status report for get methods:\n%s\n\tP99=%s - P95=%s - P50=%s\n", bg, r.getMetrics.Time.P99, r.getMetrics.Time.P95, r.getMetrics.Time.P50)

	fmt.Printf("Total error rate = %.2f%%\n", (float32(errs)/float32(sum))*100.0)
}

func sendTraffic(ctx context.Context, c *http.Client, createLimit, getLimit ratelimit.Limiter, d time.Duration) result {
	var wg sync.WaitGroup
	wg.Add(2)
	dctx, cancel := context.WithCancel(ctx)

	time.AfterFunc(d, func() {
		cancel()
	})

	var mu sync.Mutex
	orders := []int{}

	createStatus := map[int]int{}
	getStatus := map[int]int{}

	tget := tachymeter.New(&tachymeter.Config{Size: 200})
	tcreate := tachymeter.New(&tachymeter.Config{Size: 200})

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
				checkErr(err, "creating a POST request failed")

				createLimit.Take()
				start := time.Now()
				res, err := c.Do(r)
				tcreate.AddTime(time.Since(start))
				if err != nil {
					createStatus[0]++
					continue
				}
				res.Body.Close()

				createStatus[res.StatusCode]++
				mu.Lock()
				orders = append(orders, o.ID)
				mu.Unlock()
			}
		}
	}(dctx)

	go func(ctx context.Context) {
		for {
			select {
			case <-ctx.Done():
				wg.Done()
				return
			default:
				// we first get an order that we don't know if it exists or not
				order := rand.Intn(orderMax)
				status := getOrder(tget, order, getLimit, c)
				getStatus[status]++

				// now we get an order that we know exists
				randOrder := -1
				mu.Lock()
				if len(orders) > 0 {
					randOrder = orders[rand.Intn(len(orders))]
				}
				mu.Unlock()

				if randOrder != -1 {
					status = getOrder(tget, order, getLimit, c)
					getStatus[status]++
				}
			}
		}
	}(dctx)

	wg.Wait()

	return result{
		getStatus:     getStatus,
		getMetrics:    tget.Calc(),
		createStatus:  createStatus,
		createMetrics: tcreate.Calc(),
	}
}

func getOrder(tget *tachymeter.Tachymeter, orderID int, limit ratelimit.Limiter, c *http.Client) int {
	r, err := http.NewRequest(http.MethodGet, fmt.Sprintf("%s/orders/%d", *url, orderID), nil)
	if err != nil {
		log.Fatal(err)
	}

	limit.Take()
	start := time.Now()
	res, err := c.Do(r)
	tget.AddTime(time.Since(start))
	if err != nil {
		return 0
	}
	res.Body.Close()

	return res.StatusCode
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
