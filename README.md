# Link shortener: a direct-style Scala 3 service

Hands-on lab from **Scala Days 2026**: *Build a production service with VirtusLab Scala Stack and AI*.

You'll build a **link shortener** in direct-style Scala 3: plain sequential code running on virtual threads, with no `Future`s and no IO monads.

| Concern | Library |
|---|---|
| HTTP API and OpenAPI docs | [Tapir](https://tapir.softwaremill.com) (Netty sync server) |
| Concurrency, timeouts, retries, resources, errors | [Ox](https://ox.softwaremill.com) |
| SQL | [Parlance](https://github.com/lbialy/parlance) + H2, migrations with [Flyway](https://documentation.red-gate.com/fd) |
| HTTP client | [sttp client 4](https://sttp.softwaremill.com) |
| JSON | [jsoniter-scala](https://github.com/plokhotnyuk/jsoniter-scala) |
| Build | [Scala CLI](https://scala-cli.virtuslab.org) |
| Metrics | Prometheus, via [Tapir](https://tapir.softwaremill.com/en/latest/server/observability.html) (`GET /metrics`) |
| Tracing | [OpenTelemetry](https://opentelemetry.io): a span per request, trace ids in logs, propagated across threads and services |

These mostly come from the [VirtusLab Scala Stack](https://vss.virtuslab.com).

Metrics, tracing and DB migrations are already wired in for you, so you can focus on the code that matters. See [Step 0](steps/0-setup.md#production-bits-already-wired-in).

## Setup (5 minutes, please do it before the lab)

1. Install Scala CLI: <https://scala-cli.virtuslab.org/install>. On macOS: `brew install Virtuslab/scala-cli/scala-cli`.
   You don't need to install Java: Scala CLI downloads JDK 25 for you. You also need `git`, and `curl` or the IntelliJ HTTP client: [`requests.http`](requests.http) has every request from the worksheets.
2. Clone this repository: `git clone https://github.com/pjanczyk/scala-days-2026 && cd scala-days-2026`.
3. Run `scala-cli compile --test .`. It downloads the JDK and dependencies (about 300 MB), so use good Wi-Fi. When it compiles, you're all set.
4. Optional but recommended: an IDE.
   - **IntelliJ IDEA:** run `scala-cli setup-ide .` first, then open the folder.
   - **VS Code + Metals:** open the folder and import the build.
5. Optional: set up an AI coding assistant. See [AI assistants](#ai-assistants).

## How the lab works

The lab is a sequence of steps. Each step has:

- **A worksheet** in [`steps/`](steps): the commands and a cheat sheet. The tasks are the `TODO`s in the code.
- **One or two files to edit per step** (step 5, the bonus migration, spans a few), marked with `TODO`.
- **A test suite** that fails until you finish the step: `scala-cli test . --test-only '*StepN*'`.
- **A branch per step:** `step-N` starts from the reference solution of the previous step. At the start of each step, save your work and switch: `git add -A && git commit -m "My step N-1"`, then `git switch step-N`.
  - Your code stays on your local branch. For example, compare your step 2 with the reference: `git diff step-2 origin/step-3 -- src/main`.
  - The `solution` branch has everything done.

| Step | Worksheet | Topic |
|---|---|---|
| 0 | [steps/0-setup.md](steps/0-setup.md) | Run it, first green test, AI setup (optional) |
| 1 | [steps/1-tapir.md](steps/1-tapir.md) | HTTP API with Tapir and OpenAPI docs |
| 2 | [steps/2-resilience.md](steps/2-resilience.md) | Call another service with sttp: timeouts, retries, parallelism with Ox |
| 3 | [steps/3-flow.md](steps/3-flow.md) | Flows: a throttled bulk import, a background job |
| 4 | [steps/4-parlance.md](steps/4-parlance.md) | Store links in a database with Parlance |
| 5 | [steps/5-migration.md](steps/5-migration.md) | Bonus: rewrite a Future-based service in direct style, one layer at a time |

Useful commands:

```bash
scala-cli run .                                   # start the service; API docs at http://localhost:8080/docs
scala-cli test .                                  # all tests
scala-cli test . --test-only '*Step1*'            # tests of one step
scala-cli compile --test . --watch                # recompile on every change
```

## AI assistants (optional)

See VirtusLab's [`direct-style-scala` skill](https://github.com/VirtusLab/scala-skill) that teaches AI idiomatic direct-style Scala.
