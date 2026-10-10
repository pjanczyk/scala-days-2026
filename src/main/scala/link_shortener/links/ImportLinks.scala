package link_shortener.links

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import com.github.plokhotnyuk.jsoniter_scala.macros.{ConfiguredJsonValueCodec, JsonCodecMaker}
import sttp.tapir.Schema

/** Request body of `POST /api/links/import`. */
case class ImportLinks(urls: List[String]) derives ConfiguredJsonValueCodec, Schema

/** The result of importing one URL: the created link, or why it couldn't be created. */
case class ImportResult(url: String, link: Option[Link], error: Option[String]) derives Schema

object ImportResult:
  given JsonValueCodec[List[ImportResult]] = JsonCodecMaker.make
