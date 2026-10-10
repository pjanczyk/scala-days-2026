package link_shortener

import com.typesafe.scalalogging.StrictLogging
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.SdkTracerProvider
import link_shortener.links.{InMemoryLinkRepository, LinkApi, LinkService, TitleRefresher}
import link_shortener.preview.{FakePreviewServer, PreviewClient}
import ox.*
import ox.otel.context.PropagatingVirtualThreadFactory
import sttp.client4.*
import sttp.client4.logging.slf4j.Slf4jLoggingBackend
import sttp.client4.opentelemetry.OpenTelemetryTracingBackend
import sttp.client4.prometheus.PrometheusBackend

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

    val httpClientBackend = useCloseableInScope(
      DefaultSyncBackend()
        .pipe(Slf4jLoggingBackend(_)) // logging
        .pipe(OpenTelemetryTracingBackend(_, openTelemetry)) // `traceparent` header in outgoing calls
        .pipe(PrometheusBackend(_)) // metrics
    )
    // A fake external "link preview" server on a random free port
    val previewBaseUri = FakePreviewServer.start(openTelemetry, randomFailureRate = 0.3)
    val previewClient = PreviewClient(httpClientBackend, previewBaseUri)

    val linkService = LinkService(linkRepository, previewClient)
    TitleRefresher.start(linkRepository, previewClient)

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
