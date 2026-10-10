package link_shortener

import link_shortener.analytics.futures.{GeoStatsClient, ReferrerClient, WeeklyDigest}
import link_shortener.analytics.{AnalyticsService, LinkReport}
import link_shortener.links.Link
import ox.get

import java.time.Instant
import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future}

/**
 * The behaviour tests pass from the start, on the Future-based `AnalyticsService`: they are the safety net for the
 * rewrite, and must stay green. Only "report is direct style" fails until `report` returns an `Either`, not a `Future`.
 */
class Step5MigrationTest extends munit.FunSuite:
  private given ExecutionContext = ExecutionContext.global

  private def repoWith(codes: String*) =
    val repo = Fixtures.freshRepo()
    codes.zipWithIndex.foreach: (code, i) =>
      assert(repo.insert(Link(code, s"https://example.com/$code", None, i.toLong, Instant.now())).isRight)
    repo

  private def analyticsWith(codes: String*) = AnalyticsService(repoWith(codes*), GeoStatsClient(), ReferrerClient())

  /** The report, whether `report` is still Future-based or already in direct style. */
  private def reportOf(
      report: Future[Either[NotFound, LinkReport]] | Either[NotFound, LinkReport]
  ): Either[NotFound, LinkReport] =
    report match
      case future: Future[Either[NotFound, LinkReport]] @unchecked => future.get()
      case either: Either[NotFound, LinkReport] @unchecked         => either

  private def timed[T](f: => T): (T, FiniteDuration) =
    val start = System.nanoTime()
    val result = f
    (result, (System.nanoTime() - start).nanos)

  test("report is direct style: returns an Either, not a Future"):
    (analyticsWith().report("nope"): Any) match
      case Left(_: NotFound) => ()
      case other             => fail(s"expected Left(NotFound), got $other: rewrite report in direct style")

  test("report builds a report"):
    val report = reportOf(analyticsWith("scala").report("scala"))
    assertEquals(report.map(_.visitsByCountry.get("DE")), Right(Some(42L)))
    assertEquals(report.map(_.topReferrers), Right(List("scaladays.org", "vss.virtuslab.com")))

  test("report returns NotFound for an unknown code"):
    assert(reportOf(analyticsWith().report("nope")).left.exists(_.isInstanceOf[NotFound]))

  test("report calls the clients in parallel"):
    val analytics = analyticsWith("scala")
    val (_, took) = timed(reportOf(analytics.report("scala")))
    assert(took < 550.millis, s"took $took, expected both 300 ms calls to run concurrently")

  test("report skips referrers that take longer than 1 second"):
    val analytics = analyticsWith("slow")
    val (report, took) = timed(reportOf(analytics.report("slow")))
    assertEquals(report.map(_.topReferrers), Right(Nil))
    assert(took < 1500.millis, s"took $took")

  test("WeeklyDigest, a Future-based caller, still works"):
    val digest = WeeklyDigest(analyticsWith("a", "b"))
    assertEquals(digest.build(List("a", "b", "unknown")).get().map(_.code), List("b", "a"))
