package link_shortener

import io.prometheus.metrics.core.metrics.Counter
import io.prometheus.metrics.model.registry.PrometheusRegistry

/** Prometheus metrics of the app, served by Tapir at `GET /metrics`. */
object Metrics:
  // Business metrics, next to Tapir's HTTP metrics (tapir_request_total, tapir_request_duration_seconds, ...)
  val linksCreated: Counter =
    Counter.builder().name("link_shortener_links_created_total").help("Short links created").register()
  val redirects: Counter =
    Counter.builder().name("link_shortener_redirects_total").help("Redirects to original URLs").register()
