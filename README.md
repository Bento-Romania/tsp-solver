# bento-tsp-solver

A containerized Java service that finds the optimal visiting order for a set of locations using real-world road distances. It is designed for technician scheduling workflows where up to ~100 stops need to be routed efficiently.

## What it does

The service runs a two-stage pipeline:

1. **Distance matrix** — an embedded [GraphHopper](https://www.graphhopper.com/) engine computes real road distances and travel durations between every pair of input coordinates using OpenStreetMap data.
2. **TSP solve** — [Google OR-Tools](https://developers.google.com/optimization) consumes the distance matrix and finds the optimal visiting order using the Cheapest-Arc heuristic.

### API

**`POST /api/solve`** — solve TSP and return ordered route

Request:
```json
{
  "coordinates": [
    { "lat": 44.4268, "lon": 26.1025 },
    { "lat": 46.7712, "lon": 23.6236 },
    { "lat": 45.7489, "lon": 21.2087 }
  ],
  "startIndex": 0
}
```

Response:
```json
{
  "orderedCoordinates": [
    { "lat": 44.4268, "lon": 26.1025 },
    { "lat": 46.7712, "lon": 23.6236 },
    { "lat": 45.7489, "lon": 21.2087 }
  ],
  "geometry": {
    "type": "LineString",
    "coordinates": [[26.1025, 44.4268], ...]
  }
}
```

`startIndex` pins the departure point; the solver visits all other locations and returns to the start.

**`POST /api/matrix`** — compute the raw distance/duration matrix only

**`GET /api/health`** — returns `UP`, `STARTING`, or `FAILED: <reason>`

On startup the service initialises GraphHopper in a background thread. All routing endpoints return `503` until the graph is ready.

### OSM data

On first run the service checks for a pre-built graph cache at `APP_GRAPHHOPPER_GRAPH_LOCATION`. If no cache exists and no OSM file is present at `APP_GRAPHHOPPER_OSM_FILE`, it downloads the configured region from Geofabrik automatically. The default region is Romania. Building the graph from a fresh OSM file takes several minutes; subsequent starts load the cache in seconds.

If a coordinate cannot be snapped to the road network, the service falls back to Haversine straight-line distance scaled by a configurable assumed speed (default 20 km/h).

---

## How to run

### Prerequisites

- Java 21
- Maven 3.9+
- Docker (for the containerised path)

### Build

```bash
mvn package -DskipTests
```

### Run locally

```bash
mvn spring-boot:run
```

The server starts on port `8081`. On first run it will download the Romania OSM extract (~100 MB) and build the routing graph (a few minutes).

### Run tests

```bash
mvn test
```

### Docker

Build the image:
```bash
docker build -t bento-tsp-solver .
```

Run with a persistent data volume so the graph cache survives container restarts:
```bash
docker run -d \
  -p 8081:8081 \
  -v $(pwd)/data:/data \
  bento-tsp-solver
```

The container expects the OSM file and graph cache under `/data`. On first boot it downloads the OSM extract if neither the file nor a pre-built cache is present.

To use a different region, override the environment variables:
```bash
docker run -d \
  -p 8081:8081 \
  -v $(pwd)/data:/data \
  -e APP_GRAPHHOPPER_OSM_DOWNLOAD_URL=https://download.geofabrik.de/europe/romania-latest.osm.pbf \
  -e APP_GRAPHHOPPER_OSM_FILE=/data/romania-latest.osm.pbf \
  bento-tsp-solver
```

### Configuration reference

| Environment variable | Default | Description |
|---|---|---|
| `APP_GRAPHHOPPER_GRAPH_LOCATION` | `/data/graph-cache` | Path to the pre-built graph cache |
| `APP_GRAPHHOPPER_OSM_FILE` | `/data/romania-latest.osm.pbf` | Path to the OSM PBF input file |
| `APP_GRAPHHOPPER_OSM_DOWNLOAD_URL` | Geofabrik Romania | URL to download the OSM file from if it is missing |
| `APP_GRAPHHOPPER_PROFILE` | `car` | Routing profile |
| `APP_GRAPHHOPPER_THREAD_POOL_SIZE` | `8` | Parallel routing threads for matrix computation |
| `APP_GRAPHHOPPER_HAVERSINE_FALLBACK_SPEED_KMH` | `20` | Assumed speed for straight-line fallback distances |

---

## Potential future improvements

- **Multi-vehicle routing (VRP)** — OR-Tools supports Vehicle Routing Problems natively; extending the solver to handle a fleet of technicians with capacity and time-window constraints would be a natural next step.
- **Time windows** — add per-location earliest/latest arrival constraints to model appointment scheduling.
- **Async solve endpoint** — for larger inputs the solve can take a few seconds; an async pattern (submit job → poll for result) would make the API more suitable for high-concurrency deployments.
- **OpenAPI / Swagger UI** — adding `springdoc-openapi` would auto-generate interactive API docs with no extra maintenance cost.
- **Metrics and observability** — Spring Boot Actuator + Micrometer could expose solve latency, matrix computation time, and GraphHopper readiness as Prometheus metrics.
