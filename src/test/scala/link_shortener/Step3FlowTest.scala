package link_shortener

import com.github.plokhotnyuk.jsoniter_scala.core.readFromString
import link_shortener.links.{ImportResult, Link, LinkService, TitleRefresher}
import link_shortener.preview.{FakePreviewServer, Preview, PreviewClient}
import ox.*
import sttp.client4.*
import sttp.client4.testing.SyncBackendStub

import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.duration.*

class Step3FlowTest extends munit.FunSuite:
  /** A preview client that answers after `delay`, and records how many calls run at the same time. */
  private class CountingPreviewClient(delay: FiniteDuration) extends PreviewClient(SyncBackendStub, uri"http://unused"):
    private val running = AtomicInteger()
    val maxRunning = AtomicInteger()

    override def preview(url: String): Either[Unavailable, Preview] =
      maxRunning.accumulateAndGet(running.incrementAndGet(), math.max)
      sleep(delay)
      running.decrementAndGet()
      Right(Preview(Some("A title"), safe = true))

  private def urls(count: Int): List[String] = (1 to count).map(i => s"https://site$i.example.com").toList

  test("POST /api/links/import returns one result per URL, in order"):
    Fixtures.withApp(): app =>
      val body = """{"urls": ["https://scala-lang.org", "not a url", "https://malware.example.com"]}"""
      val response = basicRequest.post(uri"http://test/api/links/import").body(body).send(app.backend)
      val results = readFromString[List[ImportResult]](response.body.fold(error => fail(error), identity))
      assertEquals(results.map(_.url), List("https://scala-lang.org", "not a url", "https://malware.example.com"))
      assertEquals(results.map(_.link.isDefined), List(true, false, false))
      assert(results(1).error.exists(_.contains("Not a valid")), results(1).error)

  test("importLinks creates at most 4 links at a time"):
    val previewClient = CountingPreviewClient(delay = 1.second)
    val results = LinkService(Fixtures.freshRepo(), previewClient).importLinks(urls(8))
    assert(results.forall(_.link.isDefined), results)
    assertEquals(previewClient.maxRunning.get(), 4, "expected exactly 4 links created at the same time")

  test("importLinks creates at most 10 links per second"):
    val start = System.nanoTime()
    val results = LinkService(Fixtures.freshRepo(), CountingPreviewClient(delay = 0.millis)).importLinks(urls(25))
    val elapsed = (System.nanoTime() - start).nanos
    assert(results.forall(_.link.isDefined), results)
    assert(elapsed > 1800.millis, s"25 links in $elapsed: expected at most 10 per second")

  test("TitleRefresher fills in missing titles in the background"):
    val repo = Fixtures.freshRepo()
    assert(repo.insert(Link("scala", "https://scala-lang.org", None, 0, Instant.now())).isRight)
    val previewClient = Fixtures.previewClient(FakePreviewServer(randomFailureRate = 0.0))
    timeout(5.seconds):
      supervised:
        TitleRefresher.start(repo, previewClient, interval = 100.millis)
        Fixtures.eventually(assertEquals(repo.find("scala").flatMap(_.title), Some("Welcome to scala-lang.org")))
