# Step 2: Calling other services: sttp + Ox resilience

**Goal:** call an unreliable external service safely, using timeouts, retries and parallelism, all in plain code.

```bash
git add -A && git commit -m "My step 1"     # save your work: it stays on your branch step-1
git switch step-2                           # start from the reference solution of step 1
scala-cli test . --test-only '*Step2*'
```

## Tasks: `preview/PreviewClient.scala`

1. **`PreviewClient.fetchTitle`**: `GET /title` (a `TitleResponse`), with retries (exponential backoff from 50 ms, max 3 retries), 1 second in total, otherwise `None`.
2. **`PreviewClient.checkSafety`**: `GET /safety` (a `SafetyResponse`), with the same retries, 2 seconds in total, otherwise `Left(Unavailable(...))` (a 503).
3. **`PreviewClient.preview`**: both in parallel.

`LinkService.createLink` already uses `preview`: it rejects unsafe URLs with `BadRequest` and stores the title. Until you implement `preview`, every URL counts as safe and has no title. The `TODO`s have the details. The preview service is [`FakePreviewServer`](../src/main/scala/link_shortener/preview/FakePreviewServer.scala): it misbehaves when the URL contains `flaky`, `slow`, `lag`, `down` or `malware` (see its doc comment).

## Cheat sheet

```scala
basicRequest
  .get(uri"$baseUri/example?q=$query")
  .response(asJsonOrFail[Example])
  .send(backend)
  .body

retry(Schedule.exponentialBackoff(50.millis).maxRetries(3)) {…}
timeout(1.second) {…}                // throws on timeout
timeoutOption(1.second) {…}          // Option[T]
val (a, b) = par(opA, opB)           // both at once
```

## Try it

```bash
scala-cli run .
curl -X POST localhost:8080/api/links -d '{"url": "https://flaky.example.com"}'   # succeeds, after retries
curl -X POST localhost:8080/api/links -d '{"url": "https://slow.example.com"}'    # ~1 s, no title
curl -X POST localhost:8080/api/links -d '{"url": "https://down.example.com"}'    # 503
```

In the log, the first request has **one trace id** across both forks, every retry and the preview service.

---
✅ Done? Stretch: a `CircuitBreaker` around the safety check (`ox.resilience.CircuitBreaker`). Next: [Step 3](3-flow.md)
