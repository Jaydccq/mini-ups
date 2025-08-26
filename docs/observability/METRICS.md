Mini-UPS Tracking ID Metrics

Exposed via Spring Boot Actuator + Micrometer Prometheus.

Endpoints
- Prometheus scrape: `/actuator/prometheus`

Key Metrics
- `leaf_segment_remaining{biz_tag}`: Remaining IDs in current segment (gauge)
- `leaf_segment_generated_total{biz_tag}`: Total generated IDs (counter)
- `leaf_segment_preload_seconds_*{biz_tag}`: Preload duration histogram (timer)
- `leaf_segment_preload_successes_total{biz_tag}`: Preload successes (counter)
- `leaf_segment_preload_failures_total{biz_tag}`: Preload failures (counter)
- `leaf_segment_fallback_used_total`: Fallback generation occurrences (counter)

PromQL Examples
- Generated rate: `sum(rate(leaf_segment_generated_total[1m])) by (biz_tag)`
- Preload p95: `histogram_quantile(0.95, sum(rate(leaf_segment_preload_seconds_bucket[5m])) by (le, biz_tag))`
- Failures rate: `sum(rate(leaf_segment_preload_failures_total[5m])) by (biz_tag)`
- Remaining (current value): `leaf_segment_remaining{biz_tag="tracking_number"}`

Alert Suggestions
- Low water mark: `leaf_segment_remaining{biz_tag="tracking_number"} < <step>*0.2` for 1m
- Preload failures: `sum(rate(leaf_segment_preload_failures_total[5m])) by (biz_tag) > 0` for 5m

Grafana
- Import dashboard JSON: `docs/observability/tracking-id-dashboard.json`

