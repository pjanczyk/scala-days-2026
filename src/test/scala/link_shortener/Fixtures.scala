package link_shortener

import link_shortener.links.{InMemoryLinkRepository, LinkApi, LinkRepository, LinkService}
import link_shortener.preview.{FakePreviewServer, PreviewClient}
import ma.chinespirit.parlance.{H2, Transactor}
import org.h2.jdbcx.JdbcDataSource
import ox.tap
import sttp.client4.*
import sttp.tapir.server.stub4.TapirSyncStubInterpreter

import java.util.UUID
import scala.concurrent.duration.*

/** Everything a test needs to talk to the app. The `backend` calls the server logic directly, without a network. */
case class TestApp(backend: SyncBackend, repo: LinkRepository, upstream: FakePreviewServer)

object Fixtures:
  /** A fresh in-memory H2 database, migrated like the real one. */
  def database(name: String = s"test-${UUID.randomUUID()}"): Transactor[H2.type] =
    val dataSource = JdbcDataSource().tap(_.setURL(s"jdbc:h2:mem:$name;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"))
    DatabaseMigrations.run(dataSource)
    Transactor(H2, dataSource)

  def freshRepo(): LinkRepository = InMemoryLinkRepository()

  /** A preview client talking to an in-process [[FakePreviewServer]], without a network. */
  def previewClient(upstream: FakePreviewServer): PreviewClient =
    PreviewClient(
      TapirSyncStubInterpreter().whenServerEndpointsRunLogic(upstream.endpoints).backend(),
      uri"http://upstream"
    )

  def withApp[T]()(f: TestApp => T): T =
    val repo = freshRepo()
    val upstream = FakePreviewServer(randomFailureRate = 0.0)
    val service = LinkService(repo, previewClient(upstream))
    val endpoints = HelloApi.endpoints ++ LinkApi(service).endpoints
    val backend = TapirSyncStubInterpreter().whenServerEndpointsRunLogic(endpoints).backend()
    f(TestApp(backend, repo, upstream))

  /** Retries an assertion until it passes, for things that happen in the background. */
  def eventually[T](assertion: => T): T =
    ox.resilience.retry(ox.scheduling.Schedule.fixedInterval(50.millis).maxAttempts(40))(assertion)
