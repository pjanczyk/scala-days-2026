package link_shortener.preview

import com.github.plokhotnyuk.jsoniter_scala.macros.ConfiguredJsonValueCodec
import link_shortener.Unavailable
import ox.resilience.retry
import ox.scheduling.Schedule
import ox.{either, par, timeout}
import sttp.client4.*
import sttp.client4.jsoniter.*
import sttp.model.Uri
import sttp.tapir.Schema

import scala.concurrent.duration.*

case class Preview(title: Option[String], safe: Boolean)

case class TitleResponse(title: String) derives ConfiguredJsonValueCodec, Schema
case class SafetyResponse(safe: Boolean) derives ConfiguredJsonValueCodec, Schema

/** Talks to the external "link preview" service. */
class PreviewClient(httpClientBackend: SyncBackend, baseUri: Uri):
  private val retryPolicy = Schedule.exponentialBackoff(50.millis).maxRetries(3)

  /** Fetches the title and checks the safety of a URL, both at the same time. */
  def preview(url: String): Either[Unavailable, Preview] =
    val (title, safe) = par(fetchTitle(url), checkSafety(url))
    safe.map(Preview(title, _))

  /** The title is nice to have: if we can't get it within 1 second, we carry on without it. */
  def fetchTitle(url: String): Option[String] =
    either
      .catchAll:
        timeout(1.second):
          retry(retryPolicy):
            basicRequest
              .get(uri"$baseUri/title?url=$url")
              .response(asJsonOrFail[TitleResponse])
              .send(httpClientBackend)
              .body
              .title
      .toOption

  /** The safety check is required: if it fails (after retries), we can't accept the link. */
  def checkSafety(url: String): Either[Unavailable, Boolean] =
    either
      .catchAll:
        timeout(2.seconds):
          retry(retryPolicy):
            basicRequest
              .get(uri"$baseUri/safety?url=$url")
              .response(asJsonOrFail[SafetyResponse])
              .send(httpClientBackend)
              .body
              .safe
      .left
      .map(e => Unavailable(s"Could not check whether the URL is safe, try again later (${e.getClass.getSimpleName})"))
