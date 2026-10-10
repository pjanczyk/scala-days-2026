package link_shortener.analytics

import link_shortener.NotFound
import link_shortener.analytics.futures.{GeoStatsClient, ReferrerClient}
import link_shortener.links.{LinkRepository, ShortCode}
import ox.either.ok
import ox.{either, get, par, timeoutOption}

import java.util.concurrent.{Executors, TimeUnit, TimeoutException}
import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future, Promise}

/** Builds a link's report from two other services. Written with `Future`s, before the app moved to direct style. */
class AnalyticsService(
    linkRepository: LinkRepository,
    geoStatsClient: GeoStatsClient,
    referrerClient: ReferrerClient
)(using ExecutionContext):
  private val scheduler = Executors.newSingleThreadScheduledExecutor()

  // TODO: rewrite in direct style, returning Either[NotFound, LinkReport], with the same behaviour:
  //   both clients called in parallel, and referrers that take more than 1 second skipped (Nil).
  //   Then fix the callers the compiler points at.
  def report(code: ShortCode): Future[Either[NotFound, LinkReport]] =
    Future(linkRepository.find(code)).flatMap:
      case None       => Future.successful(Left(NotFound(s"Link '$code' not found")))
      case Some(link) =>
        val countriesF = geoStatsClient.visitsByCountry(code)
        val referrersF = withTimeout(referrerClient.topReferrers(code), 1.second).recover:
          case _: TimeoutException => Nil
        for
          countries <- countriesF
          referrers <- referrersF
        yield Right(LinkReport(link.code, link.visits, countries, referrers))

  private def withTimeout[T](future: Future[T], after: FiniteDuration): Future[T] =
    val timeoutPromise = Promise[T]()
    val fail: Runnable = () => timeoutPromise.tryFailure(TimeoutException(s"Timed out after $after")): Unit
    scheduler.schedule(fail, after.toMillis, TimeUnit.MILLISECONDS)
    Future.firstCompletedOf(List(future, timeoutPromise.future))
