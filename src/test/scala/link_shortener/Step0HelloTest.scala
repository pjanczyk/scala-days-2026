package link_shortener

import sttp.client4.*
import sttp.tapir.server.stub4.TapirSyncStubInterpreter

class Step0HelloTest extends munit.FunSuite:
  // Runs the server logic directly, without starting a server
  private val backend = TapirSyncStubInterpreter().whenServerEndpointsRunLogic(HelloApi.endpoints).backend()

  test("GET /api/hello greets the caller by name"):
    val response = basicRequest.get(uri"http://test/api/hello?name=Berlin").send(backend)
    assertEquals(response.body, Right("Hello, Berlin!"))
