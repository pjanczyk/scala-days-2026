package link_shortener.links

import com.github.plokhotnyuk.jsoniter_scala.macros.ConfiguredJsonValueCodec
import ma.chinespirit.parlance.{EntityMeta, Id, SqlNameMapper, Table}
import sttp.tapir.Schema

import java.time.Instant

/** The path of a short link: `scala` in `http://localhost:8080/scala`. */
type ShortCode = String

/** A shortened link. Also a database entity: Parlance maps it to the `link` table. */
@Table(SqlNameMapper.CamelToSnakeCase)
case class Link(
    @Id code: ShortCode,
    url: String,
    title: Option[String],
    visits: Long,
    createdAt: Instant
) derives ConfiguredJsonValueCodec,
      Schema,
      EntityMeta
