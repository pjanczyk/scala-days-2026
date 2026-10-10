package link_shortener.links

import link_shortener.{BadRequest, Conflict, NotFound}
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

  private val createLink: ServerEndpoint[Any, Identity] =
    endpoint.post
      .tag("Links")
      .summary("Shorten a URL")
      .description("Creates a short link. Pass `alias` to choose the code, otherwise a random one is generated")
      .in("api" / "links")
      .in(jsonBody[CreateLink])
      .out(statusCode(StatusCode.Created))
      .out(jsonBody[Link])
      // TODO: also map Conflict to 409
      .errorOut(oneOf[BadRequest | Conflict](badRequest))
      .handle(request => service.createLink(request))

  // TODO: GET /api/links/{code}, returning the Link as JSON
  private val getLink: ServerEndpoint[Any, Identity] =
    endpoint.handle(_ => ???)

  // TODO: GET /{code}, returning status 302 Found and the URL in the Location header
  private val redirect: ServerEndpoint[Any, Identity] =
    endpoint.handle(_ => ???)

  val endpoints: List[ServerEndpoint[Any, Identity]] = List(createLink, getLink, redirect)
