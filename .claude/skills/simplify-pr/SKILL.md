---
name: "simplify-pr"
description: "Run ponytail, pstack's debloating rules and the thermo-nuclear review in one pass over the current branch's diff, then fix what they find. Use for /simplify-pr or when asked to debloat or tighten a PR before review."
---

# Simplify PR

One command that runs three simplification passes over a change and applies the fixes. The goal is a smaller, simpler diff with identical behavior. Everything you write along the way (comments, commit text, the report) goes through the `unslop` skill.

## Scope

Use the files or diff the caller names. Otherwise use the current diff against the base branch (default `main`), including the working tree. If there is no diff, use the whole project source tree. Do not touch code outside that scope.

## Pass 1: ponytail

Invoke `ponytail:ponytail-review` on the scope (if it is not installed, apply its rule: the smallest change that fully works). Apply its findings: delete code that is not needed, reuse helpers and dependencies the codebase already has, drop options and abstractions nobody asked for. Keep validation at trust boundaries and error handling that prevents data loss.

## Pass 2: pstack

Invoke the `pstack-principles` and `pstack-practices` skills on the scope (if they are not installed, apply the summary below). Use these parts:

1. **No comments** (pstack-practices). Delete narration, banners, commented-out code and workaround sermons. Keep only legal headers, comments about behavior forced by an external dependency, public API contract docs, and issue or RFC links explaining a constraint code cannot express. Kill suppressions (`eslint-disable`, `@ts-ignore`) that hide real bugs. A surprise in our own code gets fixed in the code, not explained.
2. **TypeScript** (pstack-practices). For `.ts` and `.tsx` files apply its TypeScript rules and the type system discipline principle.
3. **Principles** (pstack-principles). Subtract before adding. Laziness protocol. Fix root causes, not symptoms. Minimize reader load. Boundary discipline. Model the domain instead of growing if/else chains. Migrate callers and delete legacy APIs in the same change. Test behavior, not implementation: delete or rewrite tests that would pass if every function returned nothing.
4. **Blast radius** (pstack-practices). For any simplification that touches shared code, find the one fact it is safe because of and check it by running code, not by reasoning alone.

## Pass 3: thermo-nuclear review

Invoke `thermo-nuclear-code-quality-review` on the result of passes 1 and 2. Apply every finding that preserves behavior, especially the presumptive blockers: a file pushed past 1000 lines, ad-hoc branching in an existing flow, thin wrappers, cast-heavy contracts, duplicated helpers, logic in the wrong layer. Look for the "code judo" reframing that deletes whole branches or layers.

Then run the thermo-nuclear security and correctness audit (pstack-practices) on the changed lines only, to confirm the simplifications did not break behavior or open a hole.

## Verify

Run the project's typecheck, lint and tests. If a simplification breaks one, revert that change and report it rather than patching around it. If the build cannot run in this environment, say so and keep edits small enough to check by reading the diff.

## Report

Keep it short: lines removed versus added, what each pass changed, anything reverted, and open items outside the scope. Do not recap the steps.