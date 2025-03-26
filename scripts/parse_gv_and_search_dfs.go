package main

import (
	"bufio"
	"fmt"
	"os"
	"strings"
)

type Edge struct {
	From string
	To   string
}

type Graph struct {
	Edges map[string][]string
}

func main() {
	if len(os.Args) < 2 {
		fmt.Println("Usage: go run main.go <path_to_graphviz_file>")
		os.Exit(1)
	}

	filename := os.Args[1]
	edges := parseGraphvizFile(filename)

	graph := buildGraph(edges)

	fmt.Println("Parsed edges:")
	for _, edge := range edges {
		fmt.Printf("%s -> %s\n", edge.From, edge.To)
	}

	fmt.Println("\nEnter two node names to find a path (or 'quit' to exit):")
	scanner := bufio.NewScanner(os.Stdin)
	for {
		fmt.Print("From node: ")
		if !scanner.Scan() {
			break
		}
		from := scanner.Text()
		if from == "quit" {
			break
		}

		fmt.Print("To node: ")
		if !scanner.Scan() {
			break
		}
		to := scanner.Text()
		if to == "quit" {
			break
		}

		path := findPath(graph, from, to)
		if len(path) > 0 {
			fmt.Printf("Path found: %s\n", strings.Join(path, " -> "))
		} else {
			fmt.Println("No path found")
		}
		fmt.Println()
	}
}

func parseGraphvizFile(filename string) []Edge {
	file, err := os.Open(filename)
	if err != nil {
		fmt.Println("Error opening file:", err)
		return nil
	}
	defer file.Close()

	var edges []Edge
	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		line := strings.TrimSpace(scanner.Text())
		if strings.Contains(line, "->") {
			parts := strings.Split(line, "->")
			if len(parts) == 2 {
				from := strings.TrimSpace(parts[0])
				to := strings.TrimSpace(strings.TrimSuffix(parts[1], ";"))
				edges = append(edges, Edge{From: from, To: to})
			}
		}
	}

	if err := scanner.Err(); err != nil {
		fmt.Println("Error reading file:", err)
	}

	return edges
}

func buildGraph(edges []Edge) Graph {
	graph := Graph{Edges: make(map[string][]string)}
	for _, edge := range edges {
		graph.Edges[edge.From] = append(graph.Edges[edge.From], edge.To)
	}
	return graph
}

func findPath(graph Graph, start, end string) []string {
	visited := make(map[string]bool)
	path := []string{}
	if dfs(graph, start, end, visited, &path) {
		return path
	}
	return []string{}
}

func dfs(graph Graph, current, end string, visited map[string]bool, path *[]string) bool {
	if visited[current] {
		return false
	}

	*path = append(*path, current)
	visited[current] = true

	if current == end {
		return true
	}

	for _, neighbor := range graph.Edges[current] {
		if dfs(graph, neighbor, end, visited, path) {
			return true
		}
	}

	*path = (*path)[:len(*path)-1]
	return false
}

