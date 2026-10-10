package link_shortener.analytics

import link_shortener.NotFound
import link_shortener.links.ShortCode
import sttp.model.StatusCode
import sttp.shared.Identity
import sttp.tapir.*
import sttp.tapir.json.jsoniter.*
import sttp.tapir.server.ServerEndpoint

class ReportApi(service: AnalyticsService):
  private val notFound: EndpointOutput.OneOfVariant[NotFound] = oneOfVariant(
    statusCode(StatusCode.NotFound).and(jsonBody[NotFound])
  )

  private val report: ServerEndpoint[Any, Identity] =
    endpoint.get
      .tag("Analytics")
      .summary("Get a link report")
      .description("Returns the link's visits, visits by country and top referrers")
      .in("api" / "links" / path[ShortCode]("code") / "report")
      .out(jsonBody[LinkReport])
      .errorOut(oneOf[NotFound](notFound))
      .handle(code => service.report(code))

  val endpoints: List[ServerEndpoint[Any, Identity]] = List(report)
