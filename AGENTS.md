# Link shortener: notes for AI coding assistants

This is a hands-on lab. The person you're helping is learning: **explain what you're about to change and why before you change it, keep diffs small, and point out the key idea in each change.**

## Build

- Build tool: **Scala CLI** (no sbt, no Mill). Dependencies are declared in `project.scala`. Don't add new ones.
- Compile: `scala-cli compile --test .`
- Test one step: `scala-cli test . --test-only '*Step3*'`, and all tests: `scala-cli test .`
- Run: `scala-cli run .`, then open http://localhost:8080/docs

## Rules

- **Never modify files under `src/test/`.** The tests define the exercise. Make them pass by changing `src/main/`.
- Work only on the TODOs of the current step (its worksheet in `steps/` names the files). Don't solve later steps.
- Direct style: no `Future`s, no IO monads, no Akka/Pekko. The exceptions are the Future-based clients in `src/main/scala/link_shortener/analytics/futures`, and step 5's starting point, which you migrate away from.
- Libraries used in this project (use these, not alternatives):
  - HTTP server: Tapir with `NettySyncServer`; server logic via `.handle` / `.handleSuccess` (not `.serverLogic`)
  - Concurrency and resilience: Ox (`supervised`, `fork`, `par`, `timeout`, `retry` with `Schedule`, `either` + `.ok()`, `Flow`)
  - SQL: **Parlance** (`ma.chinespirit.parlance`, a Magnum fork: `Transactor`, `Repo`, `sql"..."`, `xa.connect` / `xa.transact`) on H2.
  - HTTP client: sttp client 4 with `DefaultSyncBackend`
  - JSON: jsoniter-scala (`derives ConfiguredJsonValueCodec`)
  - Logging: scala-logging (`extends StrictLogging`, then `logger.info(...)`)
  - DB migrations: Flyway, SQL files in `src/main/resources/db/migration` (add a new `V<n>__<name>.sql`; never edit an applied one)
  - Metrics: Prometheus via Tapir (`Metrics.scala`); tracing: OpenTelemetry. Both are already wired in `HttpServer` and `Main`; don't change that wiring unless asked.
- Errors are values with precise types, e.g. `Either[NotFound, Link]` or `Either[BadRequest | Conflict, Link]` (the error cases are in `src/main/scala/link_shortener/ErrorInfo.scala`). Each endpoint's `errorOut` lists exactly the errors its logic returns. Don't throw for expected errors.
- Tapir: define the server logic (`.handle`) as part of each endpoint definition. No companion objects just to group definitions.
- Scala 3 style: braceless syntax, `derives`, `enum` where it fits. Code must compile without warnings.
