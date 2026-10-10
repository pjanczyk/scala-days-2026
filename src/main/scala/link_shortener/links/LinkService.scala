package link_shortener.links

import com.typesafe.scalalogging.StrictLogging
import link_shortener.{BadRequest, Conflict, Metrics, NotFound}
import ox.either
import ox.either.ok

import java.net.URI
import java.time.Clock
import scala.util.{Random, Try}

class LinkService(
    linkRepository: LinkRepository,
    clock: Clock = Clock.systemUTC()
) extends StrictLogging:

  def createLink(request: CreateLink): Either[BadRequest | Conflict, Link] =
    either:
      val url = validateUrl(request.url).ok()
      val code = request.alias match
        case Some(alias) => validateAlias(alias).ok()
        case None        => randomCode()
      val link = Link(code, url, title = None, visits = 0, createdAt = clock.instant())
      linkRepository.insert(link).ok()
      logger.info(s"Created link /${link.code} -> $url")
      Metrics.linksCreated.inc()
      link

  def getLink(code: ShortCode): Either[NotFound, Link] =
    linkRepository.find(code).toRight(NotFound(s"Link '$code' not found"))

  /** Resolves a short code to its target URL and records the visit. */
  def visitLink(code: ShortCode): Either[NotFound, String] =
    getLink(code).map: link =>
      Metrics.redirects.inc()
      linkRepository.incrementVisits(code, 1)
      link.url

  private def validateUrl(raw: String): Either[BadRequest, String] =
    Try(URI(raw)).toOption
      .filter(uri => Set("http", "https").contains(uri.getScheme) && uri.getHost != null)
      .map(_.toString)
      .toRight(BadRequest(s"Not a valid http(s) URL: '$raw'"))

  private val reservedAliases: Set[String] = Set("api", "docs", "metrics") // Paths taken by other endpoints
  private val aliasPattern = "[A-Za-z0-9_-]{3,32}".r

  private def validateAlias(alias: String): Either[BadRequest, ShortCode] =
    if !aliasPattern.matches(alias) then Left(BadRequest(s"Alias must match ${aliasPattern.regex}"))
    else if reservedAliases.contains(alias) then Left(BadRequest(s"Alias '$alias' is reserved"))
    else Right(alias)

  private def randomCode(): ShortCode =
    Random.alphanumeric.take(7).mkString
