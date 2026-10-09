---
name: "pstack-practices"
description: "pstack's hands-on coding workflows: no-comments cleanup, TDD bug fix, blast-radius check, /correct for repeated mistakes, TypeScript rules, and a thermo-nuclear security and correctness audit. Use when editing code, fixing bugs, or reviewing a branch."
---

# pstack practices

Source: cursor/plugins (pstack and thermos). Pick the section that fits the task. Write any prose you produce (comments, reports, commit text) through the unslop skill.

## No comments
Scope: the caller's files or diff, otherwise the diff against the base branch (default main) including the working tree. Do the work yourself, with a fresh read of each comment. Touch comments and identify refactor targets, never application logic, except to apply accepted fixes below.

Delete: narration, banners, commented-out code, workaround sermons.

Keep only:
- legal or license headers;
- non-obvious behavior forced by an external dependency, platform, vendor or protocol we cannot reshape (a surprise in our own code is not a keep: rename, extract, type or restructure so it is obvious without prose, and flag the symbol MUST KILL);
- prettier-ignore; lint suppressions only when the rule is faulty, pedantic or style-only;
- doc comments that define a public API contract;
- issue or RFC links that explain a constraint code cannot express.

When unsure a keep applies, delete. Kill eslint-disable, @ts-ignore and @ts-expect-error that guard real bugs or safety, and fix the code they hide. Words like IMPORTANT, do not remove, too risky, fine for now and long justifications are scent, not proof: read the nearby code and the symbol before judging. A keep survives only when proven true today on a live path in code we cannot change.

Then fix each flagged workaround at its root cause with the smallest in-scope change. Never bolt on symptom guards. If a comment says do not change X, offer the cheapest in-scope type, runtime, test or lint that enforces it, then delete the comment once encoded. Report: deletion count, restored comments, fixes, encodings offered, constraints left unenforced.

## TDD bug fix
Use when asked for TDD, a failing test or a regression test, or when the bug has an obvious cheap local test target. Skip when the test path is unclear, expensive, integration-heavy or not requested.
1. Understand the bug: intended behavior, current behavior, affected path, smallest reproduction.
2. Choose the narrowest executable check, using the test style already used for that code path. Do not build a test harness from scratch.
3. Write the smallest failing test that encodes intended behavior, not the current implementation.
4. Run it before fixing and confirm it fails for the intended reason.
5. Make the smallest production change that satisfies it.
6. Rerun and confirm it passes.
If a failing test is impractical, use the closest executable check (targeted script, repro command, snapshot, focused integration check). Prefer no new test over a bad one (mock-only, implementation-coupled, timing-dependent, expensive for a small fix). Do not change tests to match a wrong implementation or weaken assertions without cause. Report the failing-before evidence and the passing-after run.

## Blast radius
Use for what could this break or reviewing a small diff you do not trust. The job is the breakage grep will not show.
- Do not trust your own writeup. Find the one or two facts the change is safe because of and prove them by running code. Confidence ladder: said so (worthless), pointed at the line, showed the bad case cannot happen, ran it, reproduced it in the running app. Say where you stopped.
1. Read the change including the part the diff does not spell out.
2. Find the one fact it is safe because of.
3. Look where grep stops: the library source and its pinned version or local patch, timing (teardown, microtasks), JSON an API returns, DB columns, wire formats, another language reading the same bytes, feature flags, code three hops downstream.
4. For each risk give a real likelihood and cost, cite a real file:line, and list what you cleared separately. Never invent a caller or an API.
5. Prove the fact with a script or test that runs the real code and paste what happened.
Hand back: what it does, the one safety fact (proven or marked unproven), risks, cleared items, and the cheapest repro or test to run before merging.

## Correct (stop repeated mistakes)
When the same mistake keeps recurring in a repo, change the repo so it cannot recur. Assume every contributor sees only the files it opened and copies the nearest example.
1. Group recent commits, reverts, review comments and workaround comments into mistake classes. A class counts once it has happened twice.
2. Fix each class at the highest level that works: eliminate with architecture (one owner per state, one supported way per task, hide internals, one source of truth, delete old ways to copy); enforce with types; add a lint or CI check whose error names the file, type or function to use instead; test the behavior (fix or delete tests that pass if every function returned nothing); write docs or agent rules last and only for judgment calls.
3. Prove each new check fails on a real past mistake. Run the same command locally and in CI. Exceptions go on the offending line with a reason, an expiry date and a human approval.
4. Keep a table in the agent instruction file pairing each rule with what enforces it. Drop a rule once its mistake cannot happen.

## TypeScript best practices
For .ts and .tsx files. Apply the type system discipline section of the pstack-principles skill first.
| Rule | Summary |
|------|---------|
| Discriminated unions | Model variants with a kind literal so impossible states cannot be represented. No optional-field bags. |
| Branded types | Brand primitives with & { readonly __brand: "X" }. Validate once at the boundary. |
| Constructive modeling | Build the shape so the illegal value cannot be constructed ([T, ...T[]] for non-empty, start plus duration for a range). |
| Simplest total type | Keep T[] while every operation stays total. Use NonEmpty<T> only where the loose type forces !, a cast or a should-never-happen throw. |
| unknown over any | External data is unknown. |
| Schemas before guards | Use the repo's runtime schema library and infer the type (z.infer) before hand-writing a type guard. |
| No as casts | Cast only after validation. |
| Narrowing hierarchy | Discriminant switch > in > typeof/instanceof > user-defined guard > as. |
| Type guards | Must verify the claim. Name them isX or hasX. |
| Exhaustiveness | const _exhaustive: never = x; in default arms. |
| satisfies over as | Validates without widening literals. |
| Boundary validation | Parse where data crosses in, into a named domain type. Trust types inside. |
| Schema-derived types | Pick, Omit, Parameters, ReturnType, Awaited, typeof before a new interface. |
| Object args | Pass objects, not positional args. Skip on hot paths. |
| Real tests | Do not mock what you can run. Mock only what you cannot run locally. |
| Structured telemetry | Structured logger diagnostics with context. No console.log in shipped code. |

## Thermo-nuclear review (security and correctness audit)
Audit a checked-out branch for bugs, broken existing behavior and security holes. Be extremely thorough. Report only issues in code added or modified by the change, not pre-existing code.
- Breaking functionality: trace cross-package and cross-module side effects of each change.
- Breaking devex: changes to how secrets are read, env var names, ports and networking, scripts that must now be run. Adding a dependency is not a break unless it needs a manual, unusual install.
- Feature leaks: anything meant to be behind a feature flag or internal-only check must not leak.
- Intended breakage: do not report a high-risk change the branch deliberately makes with well-constrained scope, unless the author seems unaware of the implications or it looks malicious.
- Do not over-report. Misranked findings destroy trust. Trace each issue end to end before reporting. Never present unfinished research: if the backend code is available, check it.
- Only after your own audit, if there are medium or high findings and a PR exists, read the PR discussion (gh) for bot or reviewer findings, validate them, and flag the ones you include.