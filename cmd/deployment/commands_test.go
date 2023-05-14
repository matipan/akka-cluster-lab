package main

import (
	"fmt"
	"sort"
	"testing"
)

func TestGetPods(t *testing.T) {
	pods, err := getPods()
	if err != nil {
		t.Fatal(err)
	}

	sort.Sort(pods)
	fmt.Println(pods)
}

func TestGetLeaderIP(t *testing.T) {
	fmt.Println(getLeaderIP("http://localhost:8558"))
}
