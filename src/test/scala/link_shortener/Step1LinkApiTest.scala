package link_shortener

import sttp.client4.*
import sttp.model.{HeaderNames, StatusCode}

class Step1LinkApiTest extends munit.FunSuite:
  private def createLink(app: TestApp, json: String) =
    basicRequest.post(uri"http://test/api/links").body(json).contentType("application/json").send(app.backend)

  test("POST /api/links with an alias creates a link"):
    Fixtures.withApp(): app =>
      val response = createLink(app, """{"url": "https://scala-lang.org", "alias": "scala"}""")
      assertEquals(response.code, StatusCode.Created)
      assert(response.body.exists(_.contains(""""code":"scala"""")), response.body)
      assertEquals(app.repo.find("scala").map(_.url), Some("https://scala-lang.org"))

  test("POST /api/links without an alias generates a short code"):
    Fixtures.withApp(): app =>
      val response = createLink(app, """{"url": "https://scala-lang.org"}""")
      assertEquals(response.code, StatusCode.Created)
      assert(response.body.exists(_.matches(""".*"code":"[A-Za-z0-9]{7}".*""")), response.body)

  test("POST /api/links rejects an invalid URL with 400"):
    Fixtures.withApp(): app =>
      val response = createLink(app, """{"url": "not a url"}""")
      assertEquals(response.code, StatusCode.BadRequest)
      assert(response.body.left.exists(_.contains("message")), response.body)

  test("POST /api/links rejects a reserved alias with 400"):
    Fixtures.withApp(): app =>
      assertEquals(
        createLink(app, """{"url": "https://scala-lang.org", "alias": "docs"}""").code,
        StatusCode.BadRequest
      )

  test("POST /api/links rejects a taken alias with 409"):
    Fixtures.withApp(): app =>
      assertEquals(createLink(app, """{"url": "https://scala-lang.org", "alias": "dup"}""").code, StatusCode.Created)
      assertEquals(createLink(app, """{"url": "https://scala-lang.org", "alias": "dup"}""").code, StatusCode.Conflict)

  test("GET /{code} redirects to the original URL"):
    Fixtures.withApp(): app =>
      createLink(app, """{"url": "https://scala-lang.org", "alias": "scala"}""")
      val response = basicRequest.get(uri"http://test/scala").followRedirects(false).send(app.backend)
      assertEquals(response.code, StatusCode.Found)
      assertEquals(response.header(HeaderNames.Location), Some("https://scala-lang.org"))

  test("GET /{code} returns 404 for an unknown code"):
    Fixtures.withApp(): app =>
      val response = basicRequest.get(uri"http://test/nope").followRedirects(false).send(app.backend)
      assertEquals(response.code, StatusCode.NotFound)

  test("GET /api/links/{code} returns the link"):
    Fixtures.withApp(): app =>
      createLink(app, """{"url": "https://scala-lang.org", "alias": "scala"}""")
      val response = basicRequest.get(uri"http://test/api/links/scala").send(app.backend)
      assertEquals(response.code, StatusCode.Ok)
      assert(response.body.exists(_.contains(""""url":"https://scala-lang.org"""")), response.body)
