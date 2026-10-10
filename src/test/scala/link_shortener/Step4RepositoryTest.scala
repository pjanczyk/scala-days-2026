package link_shortener

import link_shortener.links.{Link, SqlLinkRepository}

import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class Step4RepositoryTest extends munit.FunSuite:
  private def freshRepo() = SqlLinkRepository(Fixtures.database())
  private val now = Instant.now().truncatedTo(ChronoUnit.MILLIS)
  private val link = Link("scala", "https://scala-lang.org", Some("The Scala Programming Language"), 0, now)

  test("insert, then find returns the same link"):
    val repo = freshRepo()
    assertEquals(repo.insert(link), Right(link))
    assertEquals(repo.find("scala"), Some(link))

  test("find returns None for an unknown code"):
    assertEquals(freshRepo().find("nope"), None)

  test("insert returns Conflict when the code is taken"):
    val repo = freshRepo()
    assert(repo.insert(link).isRight)
    assert(repo.insert(link.copy(url = "https://example.com")).left.exists(_.isInstanceOf[Conflict]))
    assertEquals(repo.find("scala").map(_.url), Some("https://scala-lang.org"))

  test("links without a title are stored too"):
    val repo = freshRepo()
    assert(repo.insert(link.copy(title = None)).isRight)
    assertEquals(repo.find("scala").flatMap(_.title), None)

  test("incrementVisits adds to the visit count"):
    val repo = freshRepo()
    assert(repo.insert(link).isRight)
    repo.incrementVisits("scala", 3)
    repo.incrementVisits("scala", 2)
    assertEquals(repo.find("scala").map(_.visits), Some(5L))

  test("the data is really in the database"):
    val name = s"step4-${UUID.randomUUID()}"
    assert(SqlLinkRepository(Fixtures.database(name)).insert(link).isRight)
    // A second repository instance on the same database sees the link
    assertEquals(SqlLinkRepository(Fixtures.database(name)).find("scala"), Some(link))
