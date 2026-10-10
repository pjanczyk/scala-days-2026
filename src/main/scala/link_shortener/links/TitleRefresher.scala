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
    // TODO: start a fork that, every `interval`, takes the links without a title (repo.findWithoutTitle),
    //   fetches their titles with previewClient.fetchTitle, at most 4 at a time, and stores them with repo.updateTitle
    ()
