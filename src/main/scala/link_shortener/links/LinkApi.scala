package link_shortener.links

import link_shortener.{BadRequest, Conflict, NotFound, Unavailable}
import sttp.model.{HeaderNames, StatusCode}
import sttp.shared.Identity
import sttp.tapir.*
import sttp.tapir.json.jsoniter.*
import sttp.tapir.server.ServerEndpoint

/** HTTP API for links. Each endpoint describes the HTTP contract (OpenAPI docs), followed by its logic in `.handle` */
class LinkApi(service: LinkService):
  private val badRequest: EndpointOutput.OneOfVariant[BadRequest] = oneOfVariant(
    statusCode(StatusCode.BadRequest).and(jsonBody[BadRequest])
  )
  private val conflict: EndpointOutput.OneOfVariant[Conflict] = oneOfVariant(
    statusCode(StatusCode.Conflict).and(jsonBody[Conflict])
  )
  private val notFound: EndpointOutput.OneOfVariant[NotFound] = oneOfVariant(
    statusCode(StatusCode.NotFound).and(jsonBody[NotFound])
  )
  private val unavailable: EndpointOutput.OneOfVariant[Unavailable] = oneOfVariant(
    statusCode(StatusCode.ServiceUnavailable).and(jsonBody[Unavailable])
  )

  private val createLink: ServerEndpoint[Any, Identity] =
    endpoint.post
      .tag("Links")
      .summary("Shorten a URL")
      .description("Creates a short link. Pass `alias` to choose the code, otherwise a random one is generated")
      .in("api" / "links")
      .in(jsonBody[CreateLink])
      .out(statusCode(StatusCode.Created))
      .out(jsonBody[Link])
      .errorOut(oneOf[BadRequest | Conflict | Unavailable](badRequest, conflict, unavailable))
      .handle(request => service.createLink(request))

  private val importLinks: ServerEndpoint[Any, Identity] =
    endpoint.post
      .tag("Links")
      .summary("Shorten many URLs")
      .description(
        "Creates a short link for each URL, at most 10 per second and 4 at a time. Returns one result per URL, in the same order"
      )
      .in("api" / "links" / "import")
      .in(jsonBody[ImportLinks])
      .out(jsonBody[List[ImportResult]])
      .handleSuccess(request => service.importLinks(request.urls))

  private val getLink: ServerEndpoint[Any, Identity] =
    endpoint.get
      .tag("Links")
      .summary("Get a link")
      .description("Returns a link with its title and visit count")
      .in("api" / "links" / path[ShortCode]("code"))
      .out(jsonBody[Link])
      .errorOut(oneOf[NotFound](notFound))
      .handle(code => service.getLink(code))

  private val redirect: ServerEndpoint[Any, Identity] =
    endpoint.get
      .tag("Links")
      .summary("Follow a short link")
      .description("Redirects to the original URL and counts the visit")
      .in(path[ShortCode]("code"))
      .out(statusCode(StatusCode.Found))
      .out(header[String](HeaderNames.Location))
      .errorOut(oneOf[NotFound](notFound))
      .handle(code => service.visitLink(code))

  val endpoints: List[ServerEndpoint[Any, Identity]] = List(createLink, importLinks, getLink, redirect)
