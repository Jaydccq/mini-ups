Mini-UPS Benchmarks

Scope
- DB write contention: before/after write-behind cache
- HTTP QPS/latency: /api/observability/qps/test
- WebSocket capacity: STOMP connections to /ws

Prereqs
- Java 17+ (javac/java)
- Backend running on http://localhost:8081 with profile `local` or via Docker
  - Local: ensure PostgreSQL on 5432 and Redis on 6380, then `./start-local.sh`
  - Docker: `./start-all.sh` (pulls images; ensure Docker running)

Commands
- HTTP QPS (e.g., 100 threads, 30s)
  - `javac scripts/benchmarks/HttpQpsBench.java`
  - `java scripts.benchmarks.HttpQpsBench 100 30`

- DB write contention (e.g., 5000 ops per run)
  - `javac scripts/benchmarks/DbWriteContentionBench.java`
  - `java scripts.benchmarks.DbWriteContentionBench 5000`
  - Output includes baseline (WB off) vs WB on: duration and databaseWrites, plus % reduction

- WebSocket STOMP capacity (e.g., 600 conns, hold 30s)
  - `javac scripts/benchmarks/WsStompLoadTest.java`
  - `java scripts.benchmarks.WsStompLoadTest 600 30`

Notes
- Debug endpoints used for benchmarks (permitAll in SecurityConfig):
  - POST `/api/debug/observability/cache/toggle?enabled=true|false`
  - POST `/api/debug/observability/cache/reset-metrics`
  - POST `/api/debug/observability/cache/stress-test-open?operations=N`
  - GET  `/api/debug/observability/cache/status`
- For claimed improvements (e.g., 40% latency reduction), provide a baseline tag/commit or config to compare; the scripts report current measurements and deltas when a baseline is defined.

