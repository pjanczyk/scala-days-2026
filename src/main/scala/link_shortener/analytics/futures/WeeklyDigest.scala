package link_shortener.analytics.futures

import link_shortener.analytics.{AnalyticsService, LinkReport}

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}

/** Legacy Future-based code that depends on [[AnalyticsService]]. Its own callers expect a `Future`: keep its API. */
class WeeklyDigest(analyticsService: AnalyticsService)(using ExecutionContext):
  // TODO: once AnalyticsService.report is in direct style, keep returning a Future here:
  //   wrap each report call in a Future that runs on virtual threads, not on the ExecutionContext of the caller
  def build(codes: List[String]): Future[List[LinkReport]] =
    Future
      .traverse(codes)(code => analyticsService.report(code))
      .map(_.flatMap(_.toOption).sortBy(-_.visits))
