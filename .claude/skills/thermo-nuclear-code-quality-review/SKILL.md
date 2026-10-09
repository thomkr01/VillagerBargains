---
name: "thermo-nuclear-code-quality-review"
description: "Run an extremely strict maintainability review for abstraction quality, giant files, and spaghetti-condition growth. Use only when asked for a thermo-nuclear or especially harsh code quality review."
---

# Thermo-Nuclear Code Quality Review

Source: cursor/plugins (thermos/skills/thermo-nuclear-code-quality-review). Use this skill for an unusually strict review focused on implementation quality, maintainability, abstraction quality, and codebase health.

Above all, push the reviewer to be **ambitious** about code structure. Do not merely identify local cleanup opportunities. Actively search for "code judo" moves: restructurings that preserve behavior while making the implementation dramatically simpler, smaller, more direct, and more elegant.

## Core Prompt

> Perform a deep code quality audit of the current branch's changes.
> Rethink how to structure / implement the changes to meaningfully improve code quality without impacting behavior.
> Work to improve abstractions, modularity, reduce Spaghetti code, improve succinctness and legibility.
> Be ambitious, if there is a clear path to improving the implementation that involves restructuring some of the codebase, go for it.
> Be extremely thorough and rigorous. Measure twice, cut once.

## Non-Negotiable Additional Standards

0. **Be ambitious about structural simplification.** Look for ways to reframe the change so whole branches, helpers, modes, conditionals, or layers disappear. Prefer the solution that feels inevitable in hindsight. If you see a path to delete complexity rather than rearrange it, push hard for it.
1. **Do not let a PR push a file from under 1k lines to over 1k lines without a very strong reason.** Prefer extracting helpers, subcomponents, or modules. If the diff crosses the threshold, ask whether the code should be decomposed first.
2. **Do not allow random spaghetti growth.** Be suspicious of ad-hoc conditionals, scattered special cases, or one-off branches in unrelated flows. Push logic into a dedicated abstraction, helper, state machine, policy object, or module.
3. **Bias toward cleaning the design, not just accepting working code.** Do not rubber-stamp "it works" implementations that leave the codebase messier. Prefer simplifications that remove moving pieces over refactors that spread the same complexity around.
4. **Prefer direct, boring, maintainable code over hacky or magical code.** Flag thin abstractions, identity wrappers, and pass-through helpers that add indirection without clarity.
5. **Push hard on type and boundary cleanliness.** Question unnecessary optionality, `unknown`, `any`, and casts. Prefer explicit typed models over loose ad-hoc objects. Do not let silent fallbacks paper over unclear invariants.
6. **Keep logic in the canonical layer and reuse existing helpers.** Call out feature logic leaking into shared paths and bespoke one-offs that duplicate existing utilities.
7. **Flag unnecessary sequential orchestration and non-atomic updates** when a cleaner structure is obvious. Do not over-index on micro-optimizations.

## Primary Review Questions

- Is there a "code judo" move that would make this dramatically simpler?
- Can this change be reframed so fewer concepts, branches, or helper layers are needed?
- Does this improve or worsen the local architecture?
- Did the diff add branching complexity where a better abstraction should exist?
- Did a cohesive module become more coupled, more stateful, or harder to scan?
- Is this logic living in the right file and layer?
- Did this change enlarge a file past a healthy size boundary?
- Do repeated conditionals signal a missing model or helper?
- Is this abstraction earning its keep, or is it just a wrapper?
- Did the diff introduce casts, optionality, or ad-hoc shapes that obscure the real invariant?
- Is orchestration more sequential or less atomic than it needs to be?

## Preferred Remedies

Delete layers of indirection; reframe the state model so conditionals disappear; change ownership boundaries; turn special cases into a simpler default flow; extract helpers or pure functions; split large files; replace condition chains with a typed model or dispatcher; separate orchestration from business logic; collapse duplicate branches; reuse canonical helpers; make type boundaries explicit; parallelize independent work; make related updates atomic.

Do not settle for "maybe rename this" when the real issue is structural. Do not settle for a cleaner version of the same messy idea if a much simpler idea is plausible.

## Review Tone

Be direct, serious, and demanding, but not rude. Do not soften major maintainability issues into mild suggestions. Examples: "this pushes the file past 1k lines. can we decompose this first?"; "this adds another special-case branch into an already busy flow. can we move this behind its own abstraction?"; "i think there's a code-judo move here that makes this much simpler."

## Output Expectations

Prioritize findings in this order: structural regressions; missed dramatic simplifications; spaghetti and branching growth; boundary, abstraction and type-contract problems; file-size and decomposition; modularity; legibility. Prefer a few high-conviction comments over a long list of nits.

## Approval Bar

Do not approve merely because behavior seems correct. Approve only with: no clear structural regression, no visible missed dramatic simplification, no unjustified file-size explosion, no spaghetti growth from special-case branching, no hacky or magical abstraction, no wrapper/cast/optionality churn, no architecture-boundary leak or duplicated canonical helper, and no missed obvious decomposition.

Presumptive blockers unless the author justifies them: preserving incidental complexity when a code-judo move would delete it; pushing a file from below 1000 to above 1000 lines; ad-hoc branching that tangles an existing flow; scattering feature checks across shared code; unnecessary abstraction or cast-heavy contracts; duplicating an existing helper or putting logic in the wrong layer.

If none of these apply, still leave explicit, actionable feedback and push for a cleaner decomposition.