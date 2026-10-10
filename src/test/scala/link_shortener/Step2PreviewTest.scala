package link_shortener

import link_shortener.preview.FakePreviewServer
import sttp.client4.*
import sttp.model.StatusCode

import scala.concurrent.duration.*

class Step2PreviewTest extends munit.FunSuite:
  private def timed[T](f: => T): (T, FiniteDuration) =
    val start = System.nanoTime()
    val result = f
    (result, (System.nanoTime() - start).nanos)

  test("fetches the title and checks safety"):
    val client = Fixtures.previewClient(FakePreviewServer(randomFailureRate = 0.0))
    assertEquals(client.preview("https://scala-lang.org").map(_.title), Right(Some("Welcome to scala-lang.org")))
    assertEquals(client.preview("https://scala-lang.org").map(_.safe), Right(true))
    assertEquals(client.preview("https://example.com/malware").map(_.safe), Right(false))

  test("retries failed calls"):
    val upstream = FakePreviewServer(randomFailureRate = 0.0)
    val client = Fixtures.previewClient(upstream)
    val url = "https://flaky.example.com"
    assertEquals(client.preview(url).map(_.title), Right(Some("Welcome to flaky.example.com")))
    assertEquals(upstream.callCount("title", url), 3, "2 failures + 1 success")
    assertEquals(upstream.callCount("safety", url), 3, "2 failures + 1 success")

  test("fetches the title and checks safety in parallel"):
    val client = Fixtures.previewClient(FakePreviewServer(randomFailureRate = 0.0))
    // With "lag" in the URL, each upstream call takes 500 ms
    val (result, took) = timed(client.preview("https://lag.example.com"))
    assert(result.isRight, result)
    assert(took < 900.millis, s"took $took, expected the two calls to run concurrently")

  test("gives up on a slow title after 1 second, and carries on without it"):
    val client = Fixtures.previewClient(FakePreviewServer(randomFailureRate = 0.0))
    val (result, took) = timed(client.preview("https://slow.example.com"))
    assertEquals(result, Right(preview.Preview(title = None, safe = true)))
    assert(took < 2.seconds, s"took $took")

  test("fails when the safety check is unavailable"):
    val client = Fixtures.previewClient(FakePreviewServer(randomFailureRate = 0.0))
    assert(client.preview("https://down.example.com").left.exists(_.isInstanceOf[Unavailable]))

  test("POST /api/links stores the page title"):
    Fixtures.withApp(): app =>
      basicRequest
        .post(uri"http://test/api/links")
        .body("""{"url": "https://scala-lang.org", "alias": "scala"}""")
        .send(app.backend)
      assertEquals(app.repo.find("scala").flatMap(_.title), Some("Welcome to scala-lang.org"))

  test("POST /api/links rejects unsafe URLs with 400"):
    Fixtures.withApp(): app =>
      val response = basicRequest
        .post(uri"http://test/api/links")
        .body("""{"url": "https://example.com/malware"}""")
        .send(app.backend)
      assertEquals(response.code, StatusCode.BadRequest)

  test("POST /api/links returns 503 when the safety check is unavailable"):
    Fixtures.withApp(): app =>
      val response =
        basicRequest.post(uri"http://test/api/links").body("""{"url": "https://down.example.com"}""").send(app.backend)
      assertEquals(response.code, StatusCode.ServiceUnavailable)
