package main

import (
	"encoding/json"
	"fmt"
	"net/http"
	"os/exec"
	"sort"
	"strings"
)

type response struct {
	Items Pods `json:"items"`
}

type Pods []Pod

func (p Pods) Len() int {
	return len(p)
}

func (p Pods) Less(i, j int) bool {
	return p[i].Status.PodIP < p[j].Status.PodIP
}

func (p Pods) Swap(i, j int) {
	p[i], p[j] = p[j], p[i]
}

type Pod struct {
	Metadata struct {
		Annotations struct {
			PodDeletionCost string `json:""`
		} `json:"annotations"`
		Name string `json:"name"`
	} `json:"metadata"`
	Status struct {
		PodIP string `json:"podIP"`
	} `json:"status"`
}

func getPods() (Pods, error) {
	result, err := exec.Command("kubectl", "-n", "orders", "get", "pods", "-o", "json").Output()
	if err != nil {
		return nil, err
	}

	res := &response{}
	return res.Items, json.Unmarshal(result, res)
}

func setPodDeletionCost(pod string, cost string) error {
	fmt.Printf("setting pod-deletion-cost=%s to pod %s\n", cost, pod)
	return exec.Command("kubectl", "-n", "orders", "annotate", "pod", pod, "controller.kubernetes.io/pod-deletion-cost="+cost).Run()
}

func restartDeployment() error {
	return exec.Command("kubectl", "-n", "orders", "rollout", "restart", "deployment/orders").Run()
}

func watchDeployment() error {
	return exec.Command("kubectl", "-n", "orders", "rollout", "status", "deployment/orders").Run()
}

func setDeploymentScenario(pods Pods, reelections int) error {
	sort.Sort(pods)

	pdc := 0
	step := 100
	for i := 0; i < reelections; i++ {
		if err := setPodDeletionCost(pods[i].Metadata.Name, fmt.Sprintf("%d", pdc)); err != nil {
			return err
		}
		pdc += step
	}

	return nil
}

type Members struct {
	Leader string `json:"leader"`
}

func getLeaderIP(host string) (string, error) {
	r, err := http.NewRequest(http.MethodGet, fmt.Sprintf("%s/cluster/members", host), nil)
	if err != nil {
		return "", err
	}

	res, err := http.DefaultClient.Do(r)
	if err != nil {
		return "", err
	}
	defer res.Body.Close()

	m := &Members{}
	if err := json.NewDecoder(res.Body).Decode(m); err != nil {
		return "", err
	}

	leader := strings.Split(strings.Split(m.Leader, "@")[1], ":")[0]
	return leader, nil
}
