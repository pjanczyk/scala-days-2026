package link_shortener.links

import link_shortener.Conflict
import java.util.concurrent.ConcurrentHashMap

trait LinkRepository:
  def find(code: ShortCode): Option[Link]
  def insert(link: Link): Either[Conflict, Link]
  def incrementVisits(code: ShortCode, by: Long): Unit

/** Keeps links in memory. Good enough for tests and for getting started. */
class InMemoryLinkRepository extends LinkRepository:
  private val links = ConcurrentHashMap[ShortCode, Link]()

  def find(code: ShortCode): Option[Link] =
    Option(links.get(code))

  def insert(link: Link): Either[Conflict, Link] =
    if links.putIfAbsent(link.code, link) == null then Right(link)
    else Left(Conflict(s"Short code '${link.code}' is already taken"))

  def incrementVisits(code: ShortCode, by: Long): Unit =
    links.computeIfPresent(code, (_, link) => link.copy(visits = link.visits + by))
