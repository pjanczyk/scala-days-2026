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

  // TODO: fetch the title and check the safety of a URL, both at the same time
  //   (until then, every URL counts as safe and has no title)
  def preview(url: String): Either[Unavailable, Preview] = Right(Preview(title = None, safe = true))

  // TODO: GET {baseUri}/title?url=..., which responds with a TitleResponse
  //   The title is nice to have: retry failures (exponential backoff from 50 ms, max 3 retries),
  //   give up after 1 second in total, and return None instead of failing.
  def fetchTitle(url: String): Option[String] = ???

  // TODO: GET {baseUri}/safety?url=..., which responds with a SafetyResponse
  //   The safety check is required: same retries, give up after 2 seconds, and return Left(Unavailable(...)) on failure.
  def checkSafety(url: String): Either[Unavailable, Boolean] = ???
