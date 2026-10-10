package link_shortener

import link_shortener.links.{InMemoryLinkRepository, LinkApi, LinkRepository, LinkService}
import sttp.client4.*
import sttp.tapir.server.stub4.TapirSyncStubInterpreter

/** Everything a test needs to talk to the app. The `backend` calls the server logic directly, without a network. */
case class TestApp(backend: SyncBackend, repo: LinkRepository)

object Fixtures:
  def withApp[T]()(f: TestApp => T): T =
    val repo = InMemoryLinkRepository()
    val service = LinkService(repo)
    val endpoints = HelloApi.endpoints ++ LinkApi(service).endpoints
    val backend = TapirSyncStubInterpreter().whenServerEndpointsRunLogic(endpoints).backend()
    f(TestApp(backend, repo))
