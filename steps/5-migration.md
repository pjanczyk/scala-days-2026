# Step 5 (bonus): From `Future`s to direct style

**Goal:** rewrite a `Future`-based service in direct style, with the same behaviour, while the `Future`-based code around it keeps working.

```bash
git add -A && git commit -m "My step 4"     # save your work: it stays on your branch step-4
git switch step-5                           # start from the reference solution of step 4
scala-cli test . --test-only '*Step5*'
```

The tests are **green from the start**, except one: they check the behaviour, and they're your safety net. "report is direct style" turns green once `report` returns an `Either` instead of a `Future`.

## Tasks: `src/main/scala/link_shortener/analytics/AnalyticsService.scala`

1. **`report`**: rewrite it in direct style: `Future[Either[NotFound, LinkReport]]` becomes `Either[NotFound, LinkReport]`. Keep the behaviour: both clients are called in parallel, and referrers that take more than 1 second are skipped (`Nil`). The `Promise`, the scheduler and the `ExecutionContext` go away.
2. **Its callers**: the compiler points at them.
   - `ReportApi`: drop the `.get()`.
   - `WeeklyDigest`: its own callers expect a `Future`, so keep its API, and run the direct-style calls on virtual threads (see its `TODO`).

The clients in `analytics/futures/Clients.scala` stay `Future`-based: pretend they're a shared library you can't change yet. Wait for them with `.get()`. **You migrate one layer at a time.**

## Cheat sheet

```scala
either:                                             // Either[E, A] from direct-style code
  repo.find(code).toRight(NotFound("...")).ok()     // the value, or return the Left
future.get()                                        // blocks (cheap on a virtual thread)
val (a, b) = par(operationA, operationB)            // both at once
timeoutOption(1.second)(operation)                  // None if it takes longer

val virtualThreadEC = ExecutionContext.fromExecutorService(
  Executors.newVirtualThreadPerTaskExecutor()
)
Future(…)(using virtualThreadEC)                    // direct-style code, called from Future-based code
```

---
✅ Done? Compare the line count and the concepts, before and after: `git diff origin/step-5 origin/solution -- src/main`
