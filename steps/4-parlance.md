# Step 4: SQL with Parlance

**Goal:** store links in SQL with type-safe queries, and see that blocking JDBC is fine on virtual threads.

```bash
git add -A && git commit -m "My step 3"     # save your work: it stays on your branch step-3
git switch step-4                           # start from the reference solution of step 3
scala-cli test . --test-only '*Step4*'
```

## Tasks: `src/main/scala/link_shortener/links/LinkRepository.scala`

1. **`SqlLinkRepository.find`**: look a link up by its code: `linkRepo.findById(…)`, or `SELECT … FROM … WHERE …`.
2. **`SqlLinkRepository.insert`**: a single insert that ignores conflicts: `linkRepo.insertAllIgnoring(…)`, or `INSERT … ON CONFLICT DO NOTHING`. Return `Left(Conflict(...))` when it inserted no row. Checking `existsById` first is racy: two requests can both pass the check.
3. **`SqlLinkRepository.incrementVisits`**: add `by` to the `visits` column with a single `UPDATE`: `linkRepo.query.where(…).increment(…)`, or `UPDATE … SET … WHERE …`. Don't read-modify-write.

`Main` already uses `SqlLinkRepository` (through `LinkRepository.apply`), so until you're done, the running app fails on most requests. `findWithoutTitle` and `updateTitle` (for the title refresher from step 3) are already done, with the query builder: use them as examples. The table is in [`V1__create_link.sql`](../src/main/resources/db/migration/V1__create_link.sql). H2 runs in memory, in PostgreSQL mode.

## Cheat sheet

```scala
xa.connect {…}                  // auto-commit
xa.transact {…}                 // transaction, rolls back on error
                                // repo and sql"" calls only compile inside one of these

repo.count                      // Long
repo.existsById(1)              // Boolean
repo.findAll                    // Vector[Entity]
repo.findById(1)                // Option[Entity]
repo.create(Entity(…))
repo.insertAllIgnoring(List(Entity(…)))                // Int
repo.save(Entity(…))
repo.query.where(…).run()                              // Vector[Entity]
repo.query.where(_.a === a).increment(_.b, 5)          // Int
repo.query.where(_.a === a).update((b = 5))            // Int

sql"SELECT a FROM entity".query[Entity].run()          // Vector[Entity]
sql"UPDATE entity SET a = $a".update.run()             // Int
sql"INSERT INTO entity (a) VALUES ($a)".update.run()   // Int
```

---
✅ Done? Stretch: try *both* the query builder and the `sql""` interpolator. Next: [Step 5 (bonus)](5-migration.md)
