# Step 1: HTTP API with Tapir

**Goal:** the core API of the link shortener, with errors mapped to proper status codes and OpenAPI docs for free.

```bash
git add -A && git commit -m "My step 0"     # save your work: it stays on your branch step-0
git switch step-1                           # start from the reference solution of step 0
scala-cli test . --test-only '*Step1*'
```

## Tasks: `src/main/scala/link_shortener/links/LinkApi.scala`

| Endpoint | Success | Errors |
|---|---|---|
| `POST /api/links` with `{"url": "...", "alias": "..."}` | `201` + the `Link` | `400` invalid URL or alias, `409` alias taken |
| `GET /api/links/{code}` | `200` + the `Link` | `404` |
| `GET /{code}` | `302` with a `Location` header | `404` |

1. **`createLink`**: add a `conflict` variant (`409`) next to `badRequest`, and list it in `errorOut`.
2. **`getLink`**: `GET /api/links/{code}`, handled by `service.getLink`, with a `notFound` variant (`404`).
3. **`redirect`**: `GET /{code}`, returning `302` and the URL in the `Location` header, handled by `service.visitLink`.

Replace each placeholder, `endpoint.handle(_ => ???)`, with the full definition: until then it matches every request.

## Cheat sheet

```scala
endpoint
  .post
  .in("api" / path[UUID]("id"))                            // path with a parameter
  .in(query[String]("query"))                              // query parameter
  .securityIn(header[String](HeaderNames.Authorization))   // input for the security logic
  .out(statusCode(StatusCode.Created))                     // status code
  .out(jsonBody[Entity])                                   // JSON response
  .out(header[String](HeaderNames.Expires))                // response header
  .errorOut(oneOf[NotFound | BadRequest](notFound, badRequest))   // the errors it can return
  .handleSecurity(apiKey => authService.authorizeUser(apiKey))
  .handle(user => (id, query) =>                           // server logic: input => Either[error, output]
    Right((Entity(...), "Mon, 18 Jul 2016"))
  )
```

## Try it

```bash
scala-cli run .
curl -i -X POST localhost:8080/api/links -d '{"url": "https://scala-lang.org", "alias": "scala"}'
curl -i localhost:8080/scala
```

Then open <http://localhost:8080/docs>: all your endpoints and error codes are documented.

---
✅ Done? Stretch: add `GET /api/links` listing all links. Next: [Step 2](2-resilience.md)
