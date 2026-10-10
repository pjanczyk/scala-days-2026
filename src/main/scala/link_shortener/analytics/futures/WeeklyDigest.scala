package link_shortener.analytics.futures

import link_shortener.analytics.{AnalyticsService, LinkReport}

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}

/** Legacy Future-based code that depends on [[AnalyticsService]]. Its own callers expect a `Future`: keep its API. */
class WeeklyDigest(analyticsService: AnalyticsService)(using ExecutionContext):
  /** Runs the blocking, direct-style calls on virtual threads, so that they don't block the caller's thread pool. */
  private val virtualThreads = ExecutionContext.fromExecutorService(Executors.newVirtualThreadPerTaskExecutor())

  def build(codes: List[String]): Future[List[LinkReport]] =
    Future
      .traverse(codes)(code => Future(analyticsService.report(code))(using virtualThreads))
      .map(_.flatMap(_.toOption).sortBy(-_.visits))
