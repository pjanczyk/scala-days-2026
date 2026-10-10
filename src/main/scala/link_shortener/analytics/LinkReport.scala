package link_shortener.analytics

import com.github.plokhotnyuk.jsoniter_scala.macros.ConfiguredJsonValueCodec
import link_shortener.links.ShortCode
import sttp.tapir.Schema

case class LinkReport(code: ShortCode, visits: Long, visitsByCountry: Map[String, Long], topReferrers: List[String])
    derives ConfiguredJsonValueCodec,
      Schema
