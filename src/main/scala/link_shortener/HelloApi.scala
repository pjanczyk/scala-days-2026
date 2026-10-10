package link_shortener

import sttp.shared.Identity
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint

object HelloApi:
  val hello: ServerEndpoint[Any, Identity] =
    endpoint.get
      .tag("Hello")
      .summary("Say hello")
      .description("Returns a greeting for the given name")
      .in("api" / "hello")
      .in(query[String]("name"))
      .out(stringBody)
      .handleSuccess(name => s"Hello, $name!")

  val endpoints: List[ServerEndpoint[Any, Identity]] = List(hello)
