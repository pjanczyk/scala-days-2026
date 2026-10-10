package link_shortener.links

import link_shortener.preview.PreviewClient
import ox.*
import ox.flow.Flow

import scala.concurrent.duration.*

/** Fetches missing titles again in the background */
object TitleRefresher:
  def start(
      repo: LinkRepository,
      previewClient: PreviewClient,
      interval: FiniteDuration = 5.seconds
  )(using Ox): Unit =
    fork:
      Flow
        .tick(interval)
        .mapConcat(_ => repo.findWithoutTitle())
        .mapPar(4)(link =>
          previewClient
            .fetchTitle(link.url) match
            case Some(title) => repo.updateTitle(link.code, title)
            case None        => ()
        )
        .runDrain()
