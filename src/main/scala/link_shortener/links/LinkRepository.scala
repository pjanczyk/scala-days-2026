package link_shortener.links

import scala.language.implicitConversions
import link_shortener.Conflict
import ma.chinespirit.parlance.*
import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters.*

trait LinkRepository:
  def find(code: ShortCode): Option[Link]
  def insert(link: Link): Either[Conflict, Link]
  def incrementVisits(code: ShortCode, by: Long): Unit
  def findWithoutTitle(): List[Link]
  def updateTitle(code: ShortCode, title: String): Unit

object LinkRepository:
  def apply(xa: Transactor[H2.type]): LinkRepository = SqlLinkRepository(xa)

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

  def findWithoutTitle(): List[Link] =
    links.values.asScala.filter(_.title.isEmpty).toList

  def updateTitle(code: ShortCode, title: String): Unit =
    links.computeIfPresent(code, (_, link) => link.copy(title = Some(title)))

/** Stores links in a SQL database through Parlance. */
class SqlLinkRepository(xa: Transactor[H2.type]) extends LinkRepository:
  private val linkRepo = Repo[Link, Link, ShortCode]()

  def find(code: ShortCode): Option[Link] =
    xa.connect:
      linkRepo.findById(code)

  def findWithoutTitle(): List[Link] =
    xa.connect:
      linkRepo.query.where(_.title.isNull).run().toList

  def insert(link: Link): Either[Conflict, Link] =
    xa.connect:
      linkRepo.insertAllIgnoring(List(link)) match
        case 1 => Right(link)
        case _ => Left(Conflict(s"Short code '${link.code}' is already taken"))

  def incrementVisits(code: ShortCode, by: Long): Unit =
    xa.connect:
      linkRepo.query.where(_.code === code).increment(_.visits, by)

  def updateTitle(code: ShortCode, title: String): Unit =
    xa.connect:
      linkRepo.query.where(_.code === code).update((title = Some(title)))

class RawSqlLinkRepository(xa: Transactor[H2.type]) extends LinkRepository:

  def find(code: ShortCode): Option[Link] =
    xa.connect:
      sql"""
        SELECT code, url, title, visits, created_at
        FROM link
        WHERE code = $code
      """.query[Link].run().headOption

  def findWithoutTitle(): List[Link] =
    xa.connect:
      sql"""
        SELECT code, url, title, visits, created_at
        FROM link
        WHERE title IS NULL
      """.query[Link].run().toList

  def insert(link: Link): Either[Conflict, Link] =
    xa.connect:
      sql"""
        INSERT INTO link (code, url, title, visits, created_at)
        VALUES (${link.code}, ${link.url}, ${link.title}, ${link.visits}, ${link.createdAt})
        ON CONFLICT DO NOTHING
      """.update.run() match
        case 1 => Right(link)
        case _ => Left(Conflict(s"Short code '${link.code}' is already taken"))

  def incrementVisits(code: ShortCode, by: Long): Unit =
    xa.connect:
      sql"""
        UPDATE link
        SET visits = visits + $by
        WHERE code = $code
      """.update.run()

  def updateTitle(code: ShortCode, title: String): Unit =
    xa.connect:
      sql"""
        UPDATE link
        SET title = $title
        WHERE code = $code
      """.update.run()
