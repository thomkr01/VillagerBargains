---
name: "pstack-principles"
description: "Engineering principles from pstack: subtract before adding, fix root causes, stay lazy, model the domain, validate at boundaries, test behavior, prove it works. Use when designing, refactoring, debugging, or reviewing code."
---

# pstack engineering principles

Source: cursor/plugins, pstack/skills/principle-*. Apply the ones that match the task. When two conflict, subtraction and root causes win.

## Subtract before you add
Remove complexity first, then build. Adding to a complex system compounds complexity.
- Sequence removal before construction. Cut before you polish.
- Design for observed usage, not speculative edge cases.
- No speculative validators, parsers or guards beyond what the spec demands.
- When a reference has no novel content, delete it instead of leaving a stub.
- Leave the design slightly simpler and more capable behind the same or smaller surface than you found it.

## Laziness protocol
Most result with least code and complexity.
- Prefer deletion. When asked to refactor or improve, look for removals before additions.
- Keep a flat call hierarchy. If answering a question means tracing more than 3 files or layers, flatten it. A rich interface that hides real work is not a deep chain.
- Consolidate decisions: one source of truth, passed on as a simple flag.
- Minimize the diff. Fewer lines beat elegant boilerplate.
- Question the threading: if a task asks you to pass a new signal through types, schemas or pipelines, look for a more direct path.
- Remove tiny pass-throughs, representation leaks and duplicated choices before they spread.
- Test: would a human developer find this exhausting to maintain? Then it is a bad solution.

## Fix root causes
Do not fix symptoms.
- Reproduce first. Ask why until you hit the root cause.
- Do not add guards. A nil check that silences a crash is a symptom fix.
- If a workaround needs a paragraph-long comment, the code is wrong. Fix the code.
- Check for the pattern, not just the instance: grep for the same pattern and fix all of them.
- When stuck, instrument. Add logging, read the actual error. Do not guess.
- Failure after restart: suspect stale persistent state (config, caches, lock files, serialized state) before code.

## Attack the premise
When two or more fixes that share one premise fail the same gate, suspect the premise.
- Write the premise down: the one sentence every failed fix assumed.
- Take a census (a rerunnable script) of which actors hold the imbalance before the next fix.
- If the same few actors hold most of it every run, find what assigns them that role and remove the asymmetry instead of compensating for it.
- If the census is even, the premise is not the cause. Look elsewhere.

## Redesign from first principles
When integrating a new requirement, do not bolt it on.
- Read all affected files. Ask: if we wrote this from scratch with this requirement, what would we build?
- Propagate the change through every reference: types, docs, examples, rationale.
- Think about the whole redesign, then deliver it incrementally.

## Exhaust the design space
For a novel interaction or architecture decision with no precedent, build 2 or 3 competing prototypes or sketches and compare them before committing. A second flavor of the first shape does not count. Skip for mechanical work, bug fixes, and single-viable-approach changes.

## Foundational thinking
- Get data structures right before writing logic: define core types early, trace every access pattern.
- DRY the structure, not every line. Three similar statements beat a premature abstraction. Prefer explicit over clever.
- Scaffold first when it helps every later phase (CI, lint, test infrastructure, shared types). Setup before features, tests before fixes. Keep commits small and single-purpose.
- Before sharing state between actors, ask what happens if another actor modifies it concurrently. If not nothing, isolate.
- Subtraction comes before scaffolding.

## Model the domain
Encode the real domain in a structure instead of scattered conditionals: a state machine instead of booleans, a typed object instead of loose params, a map or discriminated union instead of branching across files, a reducer instead of ad hoc mutation, a module organized around one body of domain knowledge (execution order is not ownership).
- Do not force an abstraction. Prefer boring code if the shape is already clear and local.
- Sign you skipped this: a feature grows an if/else chain by one branch, or a second boolean must stay in sync with the first.

## Minimize reader load
Track layers to trace and state to hold.
- Collapse wrappers with one caller, adapters with no second implementation, speculative indirection. Inline them.
- Adjacent layers must change the abstraction. Pass-through layers add load without compression.
- Prefer boundaries that hide meaningful decisions over broad interfaces that hide little.
- Shrink state scope: pure functions over mutation, locals over fields, fields over module state, module state over globals. Derive instead of sync.
- Name the invariant at the boundary, not in every consumer.
- Test: can a new reader answer where X comes from and what can change X in under 30 seconds?

## Boundary discipline
Validate, narrow types and handle errors at system boundaries (CLI args, config, network, external APIs). Trust internal types. Keep business logic in pure functions and the framework shell thin.
- Parse raw data into domain types at the boundary. No redundant nil checks deep in call chains.
- Do not re-export transport, storage, framework or wire types through the public surface.
- Tests: is this data crossing a boundary right now? If not, validation is redundant. Can this be a pure function the shell just calls? Then extract it.

## Type system discipline
The type checker is a proof assistant.
- Make illegal states unrepresentable: sum types or discriminated unions, not a bag of optional fields. If you must comment when a combination is valid, the type is too loose.
- Types are constructions: build the shape so the illegal value cannot be built (non-empty list as head plus rest, a range as start plus duration).
- Brand semantic primitives (UserId vs OrderId). Validate once at creation.
- External data is untyped until parsed at the boundary.
- Do not lie to the type system: casts and assertion functions are latent crashes.
- Exhaustive matching must fail compilation when a variant is added.
- Derive types from authoritative schemas instead of hand-rolling parallel ones.
- Strengthen a type only where partiality appears (a throw, a null check, a should-never-happen). Then stop.

## Test behavior, not implementation
A test calls the code the way users do and asserts the observed result against a literal expected value. Before keeping a test, ask whether it would still pass if every imported function returned undefined. If yes, rewrite the assertion or delete the test.
- Shapes that pass anyway: weak or no assertion (toBeDefined, toBeTruthy, not.toThrow); mock or absence only (toHaveBeenCalled, toEqual([])); self-referential (expect(f(a)).toBe(f(a))); constant pin (restating a hand-maintained constant or prompt); fixture asserts fixture.
- Fix: call the subject in the test body with one concrete input and assert the literal output or observable effect. For a mock, assert the payload it received or the state after, not that it was called.
- Keep tests of relations across table rows and compile-time type tests.

## Prove it works
Verify against the real artifact, not a proxy, a self-report or it compiles.
- Read the actual value. Check liveness directly. When verification fails, suspect the observation method before the system.
- Script the check so a reviewer can re-run it. Commit it only for large work that needs an audit trail.

## Sequence work into verifiable units
Break multi-step work (sweeps, migrations, runs of similar edits) into small units that each end in a checkable state. Do not advance until the current one is green. Stack commits so the sequence proves itself: failing test first, then the fix; subtraction before the reshape; scaffold before the feature.

## Migrate callers, then delete legacy APIs
When a new internal API is the right design, inventory callers, migrate them and delete the old API in the same wave. Treat temporary adapters as exceptional and time-boxed. Update tests to the new contract and delete tests that only protect pre-refactor details. Applies when no external users depend on backward compatibility.

## Make operations idempotent
For commands, lifecycle steps and processing loops: what happens if this runs twice, and if the previous run crashed halfway? Re-execution must converge to the same end state. Scan for existing state and clean stale artifacts on startup, compare by content equivalence, use self-healing locks (PID-based stale detection), and respawn failed work cleanly.

## Separate before serializing shared state
When concurrent actors might write the same file, branch, key or object, first ask whether they need the same mutable object. Default: give each actor its own owned file, key or branch and merge only at the read boundary. Serialize structurally (lockfile, sequential phase, single-writer actor, compare-and-swap) only when one shared writer is a real invariant. Conventions are not concurrency control.

## Encode lessons in structure
When you write the same instruction a second time, ask whether it can be a lint rule, a metadata flag, a runtime check or a script. If yes, encode it and delete the instruction. Pick the strongest mechanism: an unrepresentable state, then a lint or banned API that fails CI, then a canonical helper, then a runtime check. If it needs judgment, make the instruction prominent and add an example of the failure. Capture every correction and route it: one-off, recurring fix, or systemic principle.