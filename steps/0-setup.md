# Step 0: Setup and a first green test

**Goal:** a running service, a working toolchain, and your first passing test.

## 1. Run it

```bash
git switch step-0       # you'll commit your work for this step here
scala-cli run .
```

Then open <http://localhost:8080/docs>. That's Swagger UI, generated from the endpoint definitions.
Try `GET /api/hello`. It fails with a 500 error, because the server logic is still a `???`.

## 2. Make the test pass

```bash
scala-cli test . --test-only '*Step0*'
```

Open `src/main/scala/link_shortener/HelloApi.scala` and do the `TODO`, so that `GET /api/hello?name=Berlin` returns `Hello, Berlin!`.

Things to notice:

- `endpoint.get.in(...).out(...)` is a **value** describing the endpoint. Nothing runs yet.
- `.handleSuccess(name => ...)` attaches the server logic: a plain function. There's no `Future` and no `IO`.
- `Main` extends `OxApp.Simple`. Its `run` is a normal method; resources are released in reverse order when it ends (Ctrl+C).

## 3. Set up your AI assistant (optional)

Teach it the idioms with VirtusLab's **direct-style-scala** skill (Ox, Tapir, sttp): install it as in [README → AI assistants](../README.md#ai-assistants). It reads the project's rules from `AGENTS.md`.

## 4. Production bits already wired in

You won't write these today, but a production service needs them, so they're already there. Take a look while the service is running:

- **Metrics:** `curl localhost:8080/metrics`. That's Prometheus format, collected by a Tapir interceptor for every endpoint (`tapir_request_total`, `tapir_request_duration_seconds`, ...). Business metrics such as `link_shortener_links_created_total` are added in [`Metrics.scala`](../src/main/scala/link_shortener/Metrics.scala).
- **Tracing:** every request gets an OpenTelemetry span, and each log line shows its trace id: `[trace 4bf92f35]`. In `Main.settings`, Ox is told to start every fork with the tracing context of its parent thread, so the trace id follows your code across threads. You'll see this in step 2.
- **DB migrations:** from step 4, the schema lives in versioned SQL files in [`src/main/resources/db/migration`](../src/main/resources/db/migration), applied by Flyway on startup.

The server wiring (docs, metrics, tracing) is in [`HttpServer.scala`](../src/main/scala/link_shortener/HttpServer.scala).

---
✅ Done? Next: [Step 1](1-tapir.md). Stuck? Ask at any time.
