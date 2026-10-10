package link_shortener.analytics

import link_shortener.NotFound
import link_shortener.analytics.futures.{GeoStatsClient, ReferrerClient}
import link_shortener.links.{LinkRepository, ShortCode}
import ox.either.ok
import ox.{either, get, par, timeoutOption}

import scala.concurrent.duration.*

/**
 * Builds a link's report from two other services, in direct style.
 *
 * The clients are still Future-based: we just wait for their results with `.get()`. Migration can go one layer at a
 * time.
 */
class AnalyticsService(
    linkRepository: LinkRepository,
    geoStatsClient: GeoStatsClient,
    referrerClient: ReferrerClient
):
  def report(code: ShortCode): Either[NotFound, LinkReport] = either:
    val link = linkRepository.find(code).toRight(NotFound(s"Link '$code' not found")).ok()
    val (countries, referrers) = par(
      geoStatsClient.visitsByCountry(code).get(),
      timeoutOption(1.second)(referrerClient.topReferrers(code).get()).getOrElse(Nil)
    )
    LinkReport(link.code, link.visits, countries, referrers)
