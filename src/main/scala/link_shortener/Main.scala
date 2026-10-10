package link_shortener

import com.typesafe.scalalogging.StrictLogging
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.SdkTracerProvider
import link_shortener.links.{InMemoryLinkRepository, LinkApi, LinkService}
import ox.*
import ox.otel.context.PropagatingVirtualThreadFactory

object Main extends OxApp.Simple, StrictLogging:

  override protected def settings: OxApp.Settings = OxApp.Settings.Default.copy(
    // Every fork (e.g. request handlers, par) inherits tracing context of the thread that started it
    threadFactory = Some(PropagatingVirtualThreadFactory())
  )

  def run(using Ox): Unit =
    releaseAfterScope:
      logger.info("Shutdown completed")

    val openTelemetry = useCloseableInScope(
      OpenTelemetrySdk
        .builder()
        .setTracerProvider(SdkTracerProvider.builder().build())
        .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
        .build()
    )

    val linkRepository = InMemoryLinkRepository()
    val linkService = LinkService(linkRepository)

    val apiEndpoints = HelloApi.endpoints ++ LinkApi(linkService).endpoints

    releaseAfterScope:
      logger.info("HTTP server stopped")

    val binding = HttpServer.start(apiEndpoints, openTelemetry, port = 8080)
    logger.info(s"Link shortener is running.")
    logger.info(s"API docs: http://localhost:${binding.port}/docs")
    logger.info(s"Metrics: http://localhost:${binding.port}/metrics")

    releaseAfterScope:
      logger.info("Shutting down application")

    never
