package link_shortener.links

import com.github.plokhotnyuk.jsoniter_scala.macros.ConfiguredJsonValueCodec
import sttp.tapir.Schema

/** Request body of `POST /api/links`. Without an alias, a random short code is generated. */
case class CreateLink(
    url: String,
    alias: Option[String] = None
) derives ConfiguredJsonValueCodec,
      Schema
