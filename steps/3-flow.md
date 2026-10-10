# Step 3: Ox Flows and background jobs

**Goal:** process many items at a limited rate and with a limited parallelism using Ox `Flow`, in a request and in a background fork.

```bash
git add -A && git commit -m "My step 2"     # save your work: it stays on your branch step-2
git switch step-3                           # start from the reference solution of step 2
scala-cli test . --test-only '*Step3*'
```

## Tasks: `links/LinkService.scala` and `links/TitleRefresher.scala`

1. **`LinkService.importLinks`** (behind `POST /api/links/import`): create the links at most 10 per second (`throttle`) and at most 4 at a time (`mapPar`), with a `Flow`. Keep the results in the order of the URLs.
2. **`TitleRefresher.start`**: start a `fork` that, every `interval`, takes the links without a title (`repo.findWithoutTitle()`), fetches their titles with `previewClient.fetchTitle`, at most 4 at a time, and stores them with `repo.updateTitle`. `Main` already starts it.

## Cheat sheet

```scala
supervised:                                   // concurrency scope, provides implicit `Ox`
  fork:                                       // daemon thread: cancelled at the end of scope
    doSomething()

Flow.fromIterable(List(1, 2, 3))
Flow.tick(1.second)                           // emits every second, forever

flow.throttle(10, 1.second)                   // at most 10 per second
flow.map(element => ...)
flow.mapPar(4)(element => ...)                // 4 at a time, in order
flow.mapConcat(element => List(...))          // 1 → many
flow.runToList()                              // List[T]
flow.runDrain()                               // Unit, runs until cancelled
```

In `TitleRefresher.start`, the scope is already there: `(using Ox)` is the app's scope.

## Try it

```bash
scala-cli run .
curl -X POST localhost:8080/api/links/import \
  -d '{"urls": ["https://scala-lang.org", "nope", "https://lag1.example.com", "https://lag2.example.com"]}'
curl -X POST localhost:8080/api/links -d '{"url": "https://slow2.example.com", "alias": "slow"}'   # no title
sleep 6 && curl localhost:8080/api/links/slow                                                      # title is there
```

The first `/title` call for a `slow` URL takes 5 seconds, so the link is created without a title; the refresher fetches it later.

---
✅ Done? Stretch: what would change if `importLinks` used `mapParUnordered` instead of `mapPar`, and when would you want that? Next: [Step 4](4-parlance.md)
