package link_shortener

import link_shortener.links.{InMemoryLinkRepository, LinkApi, LinkRepository, LinkService}
import link_shortener.preview.{FakePreviewServer, PreviewClient}
import sttp.client4.*
import sttp.tapir.server.stub4.TapirSyncStubInterpreter

/** Everything a test needs to talk to the app. The `backend` calls the server logic directly, without a network. */
case class TestApp(backend: SyncBackend, repo: LinkRepository, upstream: FakePreviewServer)

object Fixtures:
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
