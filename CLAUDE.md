# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

This repository is currently empty (only `LICENSE`, `README.md`, and `.gitignore` exist) — no source code, build tooling, or tests have been written yet. There are no commands to build, lint, or test until the project is scaffolded.

The project will be implemented in **Java** (GraphHopper itself is a Java library/server, and OR-Tools has Java bindings, so a single-JVM service is the expected shape — likely Maven or Gradle once scaffolded).

## Intended purpose

A containerized service that solves the Traveling Salesman Problem (TSP) for a technician scheduling use case ("Technician TSP scheduler", per README):

1. **Input**: a list of ~20 locations (lat/lon coordinates).
2. **Distance/route calculation**: a GraphHopper engine instance computes real-world routes and distances/durations between each pair of points (not straight-line distance).
3. **TSP solve**: Google OR-Tools consumes the GraphHopper distance matrix and solves for the optimal visiting order of all points.
4. **Output**: the container returns
   - a geometry (e.g. GeoJSON/polyline) representing the full ordered route, and
   - an ordered array of the input points reflecting the solved visiting order.

## Architecture notes for future implementation

- This is fundamentally a two-engine pipeline: **GraphHopper** (routing/distance matrix) feeds **OR-Tools** (combinatorial optimization). Keep these as distinct stages — GraphHopper produces the pairwise cost matrix, OR-Tools only operates on that matrix and has no knowledge of geography.
- GraphHopper requires its own routing graph/OSM data to be built or mounted; this is a separate concern from the OR-Tools solving logic and likely needs its own setup step inside the container (or a sidecar/separate service).
- The final response needs to reconcile two outputs from different stages: the OR-Tools point order and the GraphHopper-derived geometry for the route between consecutive points in that order — the geometry should be assembled by stitching together the GraphHopper route segments in the OR-Tools-determined order, not recomputed independently.
