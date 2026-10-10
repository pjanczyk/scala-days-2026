package link_shortener.preview

import com.typesafe.scalalogging.StrictLogging
import io.opentelemetry.api.OpenTelemetry
import ox.*
import sttp.model.StatusCode
import sttp.shared.Identity
import sttp.tapir.*
import sttp.tapir.json.jsoniter.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.netty.sync.{NettySyncServer, NettySyncServerBinding, NettySyncServerOptions}
import sttp.tapir.server.tracing.opentelemetry.OpenTelemetryTracing
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.duration.*
import scala.util.Random
import sttp.client4.UriContext
import sttp.model.Uri
import sttp.tapir.server.netty.NettyConfig

/**
 * A stand-in for an external "link preview" service, so that the lab doesn't depend on the internet.
 *
 * Like a real dependency, it is sometimes slow and sometimes fails. The behaviour is driven by the URL being checked:
 *   - `slow` in the URL: the first `/title` call takes 5 seconds, later ones are fast
 *   - `lag` in the URL: both `/title` and `/safety` take 500 ms
 *   - `flaky` in the URL: the first 2 calls to each endpoint fail with 503
 *   - `down` in the URL: `/safety` always fails with 503
 *   - `malware` in the URL: `/safety` says it's unsafe
 *   - anything else: fails randomly with probability `randomFailureRate`
 */
class FakePreviewServer(randomFailureRate: Double) extends StrictLogging:
  private val calls = ConcurrentHashMap[(String, String), AtomicInteger]()

  /** How many times an endpoint (`title` or `safety`) was called for a given URL. */
  def callCount(endpointName: String, url: String): Int =
    Option(calls.get((endpointName, url))).fold(0)(_.get())

  private def respond[T](endpointName: String, url: String)(response: => T): Either[StatusCode, T] =
    val attempt = calls.computeIfAbsent((endpointName, url), _ => AtomicInteger()).incrementAndGet()
    logger.info(s"Preview service: /$endpointName for $url (attempt $attempt)")
    if url.contains("lag") then sleep(500.millis)
    if url.contains("slow") && endpointName == "title" && attempt == 1 then sleep(5.seconds)
    if url.contains("flaky") && attempt <= 2 then Left(StatusCode.ServiceUnavailable)
    else if !url.contains("flaky") && Random.nextDouble() < randomFailureRate then Left(StatusCode.ServiceUnavailable)
    else Right(response)

  private val titleEndpoint =
    endpoint.get
      .in("title")
      .in(query[String]("url"))
      .out(jsonBody[TitleResponse])
      .errorOut(statusCode)
      .handle: url =>
        respond("title", url):
          TitleResponse(s"Welcome to ${Option(URI(url).getHost).getOrElse(url)}")

  private val safetyEndpoint =
    endpoint.get
      .in("safety")
      .in(query[String]("url"))
      .out(jsonBody[SafetyResponse])
      .errorOut(statusCode)
      .handle: url =>
        if url.contains("down") then Left(StatusCode.ServiceUnavailable)
        else respond("safety", url)(SafetyResponse(safe = !url.contains("malware")))

  val endpoints: List[ServerEndpoint[Any, Identity]] = List(titleEndpoint, safetyEndpoint)

object FakePreviewServer:

  def start(
      otel: OpenTelemetry,
      randomFailureRate: Double = 0.0
  )(using Ox): Uri =
    val server =
      NettySyncServer(
        NettySyncServerOptions.customiseInterceptors
          .prependInterceptor(OpenTelemetryTracing[Identity](otel)) // `traceparent` header propagation
          .options,
        NettyConfig.default
          .port(0) // random port
          .withGracefulShutdownTimeout(500.millis)
          .withDontShutdownEventLoopGroupOnClose
      )
        .addEndpoints(FakePreviewServer(randomFailureRate).endpoints)
    val binding = useInScope(server.start())(_.stop())
    uri"http://localhost:${binding.port}"
