# Thanos Querier

Interfaz de consulta Thanos para explorar métricas federadas de Prometheus. Permite ejecutar queries PromQL directamente contra el store de métricas.

<iframe src="https://thanos-querier.apps.cluster-l9nhj.dynamic.redhatworkshops.io/" width="100%" height="900" frameborder="0" style="border:1px solid #ddd; border-radius:8px;"></iframe>

[Abrir Thanos Querier :material-open-in-new:](https://thanos-querier.apps.cluster-l9nhj.dynamic.redhatworkshops.io){ .md-button }

## Queries útiles

### Tráfico HTTP total en el mesh

```promql
sum(rate(istio_requests_total{reporter="destination"}[5m])) by (destination_workload_namespace)
```

### Success rate por servicio

```promql
sum(rate(istio_requests_total{response_code=~"2.*", reporter="destination"}[5m])) by (destination_workload)
/
sum(rate(istio_requests_total{reporter="destination"}[5m])) by (destination_workload)
```

### Latencia P95

```promql
histogram_quantile(0.95, sum(rate(istio_request_duration_milliseconds_bucket{reporter="destination"}[5m])) by (le, destination_workload))
```

### TCP bytes por servicio (desde ztunnel)

```promql
sum(rate(istio_tcp_sent_bytes_total{reporter="destination"}[5m])) by (destination_workload)
```

### Rate limit rejections (Kuadrant)

```promql
sum(increase(ratelimit_service_rate_limit_total{status="over_limit"}[1h]))
```

## Datasource

- **Servicio interno**: `thanos-querier-querier:10902`
- **Puerto**: 10902 (HTTP query API)
- Federa métricas del MonitoringStack `stack` (Prometheus con 7d de retención)
