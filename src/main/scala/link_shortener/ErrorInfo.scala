package link_shortener

import com.github.plokhotnyuk.jsoniter_scala.macros.ConfiguredJsonValueCodec
import sttp.tapir.Schema

/** Errors returned by the API. Each one maps to an HTTP status code in `LinkApi`. */
sealed trait ErrorInfo:
  def message: String

case class NotFound(message: String) extends ErrorInfo derives ConfiguredJsonValueCodec, Schema
case class BadRequest(message: String) extends ErrorInfo derives ConfiguredJsonValueCodec, Schema
case class Conflict(message: String) extends ErrorInfo derives ConfiguredJsonValueCodec, Schema
case class Unavailable(message: String) extends ErrorInfo derives ConfiguredJsonValueCodec, Schema
