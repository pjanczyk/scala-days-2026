package link_shortener.links

import com.github.plokhotnyuk.jsoniter_scala.macros.ConfiguredJsonValueCodec
import sttp.tapir.Schema

import java.time.Instant

/** The path of a short link: `scala` in `http://localhost:8080/scala`. */
type ShortCode = String

/** A shortened link. */
case class Link(
    code: ShortCode,
    url: String,
    title: Option[String],
    visits: Long,
    createdAt: Instant
) derives ConfiguredJsonValueCodec,
      Schema
