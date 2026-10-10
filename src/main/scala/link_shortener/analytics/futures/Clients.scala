package link_shortener.analytics.futures

import scala.concurrent.{ExecutionContext, Future, blocking}

// Imagine these clients come from a shared library that other (Future-based) teams still use: you can't change them yet.

class GeoStatsClient:
  /** Visits per country. Takes ~300 ms. */
  def visitsByCountry(code: String): Future[Map[String, Long]] = Future {
    blocking(Thread.sleep(300))
    Map("DE" -> 42L, "PL" -> 17L, "CH" -> 5L)
  }(using ExecutionContext.global)

class ReferrerClient:
  /** Top referring sites. Usually takes ~300 ms, but is very slow for codes containing "slow". */
  def topReferrers(code: String): Future[List[String]] = Future {
    blocking(Thread.sleep(if code.contains("slow") then 5000 else 300))
    List("scaladays.org", "vss.virtuslab.com")
  }(using ExecutionContext.global)
