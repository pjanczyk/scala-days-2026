package link_shortener

import io.opentelemetry.api.OpenTelemetry
import ox.{useInScope, Ox}
import sttp.shared.Identity
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.metrics.prometheus.PrometheusMetrics
import sttp.tapir.server.netty.sync.{NettySyncServer, NettySyncServerBinding, NettySyncServerOptions}
import sttp.tapir.server.tracing.opentelemetry.OpenTelemetryTracing
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import scala.concurrent.duration.*
import sttp.tapir.server.netty.NettyConfig

/**
 * The HTTP server: the API, OpenAPI docs at `/docs`, Prometheus metrics at `/metrics`, and a tracing span per request.
 */
object HttpServer:
  def start(
      apiEndpoints: List[ServerEndpoint[Any, Identity]],
      otel: OpenTelemetry,
      port: Int
  )(using Ox): NettySyncServerBinding =
    val docsEndpoints = SwaggerInterpreter().fromServerEndpoints[Identity](apiEndpoints, "Link Shortener", "1.0")
    val metrics = PrometheusMetrics.default[Identity]()
    val options = NettySyncServerOptions.customiseInterceptors
      .prependInterceptor(OpenTelemetryTracing[Identity](otel))
      .metricsInterceptor(metrics.metricsInterceptor(ignoreEndpoints = docsEndpoints.map(_.endpoint)))
      .serverLog(NettySyncServerOptions.defaultServerLog)
      .options
    val config = NettyConfig.default
      .port(port)
      .withGracefulShutdownTimeout(500.millis)
      .withDontShutdownEventLoopGroupOnClose
    val server = NettySyncServer(options, config)
      .addEndpoints(metrics.metricsEndpoint :: docsEndpoints ++ apiEndpoints)
    useInScope(server.start())(_.stop())
