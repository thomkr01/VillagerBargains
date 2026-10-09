---
name: "unslop"
description: "Always apply to any prose you write or edit, including code comments, docstrings, commit messages, PR text, READMEs, docs, error and log messages, and replies. Cuts AI tells like filler, stock AI words, em dashes and sycophancy."
---

# Unslop

Source: cursor/plugins, pstack/skills/unslop. This skill always applies to prose. Before you finish any text (a comment, a commit message, a PR body, a doc, a reply), scan it for the patterns below and rewrite. Preserve meaning, match the intended tone. It cleans writing. For AI-style bloat in the code itself, use ponytail, pstack-practices (no comments) and the thermo-nuclear review.

## Content
- **Superficial -ing phrases.** highlighting, ensuring, reflecting, showcasing, fostering. Delete or expand with real sources.
- **Vague attributions.** Experts believe, industry reports suggest. Name the source or delete.

## Language
- **AI vocabulary.** Additionally, crucial, delve, enduring, enhance, fostering, garner, interplay, intricate, landscape (abstract), pivotal, showcase, tapestry (abstract), testament, underscore, vibrant. Use plain words.
- **Fancy ways to say is.** serves as, stands as, boasts, features. Say is or has.
- **Not just X, but Y.** State the point directly.
- **Rule of three.** Do not force groups of three. Use the natural number.
- **Synonym cycling.** Pick one word and repeat it.
- **False ranges.** from X to Y where X and Y are not on a meaningful scale. List the topics.

## Style
- **Em dash overuse.** Avoid em dashes. Use periods or commas.
- **Colon overuse.** Fine before a list or example. Not as a mid-sentence connector.
- **Boldface overuse.** Do not bold every proper noun or acronym.
- **Inline-header lists.** A bold label and colon that restates the line becomes prose. A bold lead-in ending in a period, followed by new detail, is fine.
- **Title case headings.** Use sentence case.
- **Decorative emojis.** Remove from headings and bullets.
- **Curly quotes.** Use straight quotes.

## Communication artifacts
- **Chatbot phrases.** I hope this helps, Let me know if, Of course, Certainly. Remove.
- **Sycophantic tone.** Great question, you are absolutely right. Respond directly.

## Filler
- **Filler phrases.** In order to becomes To. Due to the fact that becomes Because. It is important to note that gets deleted.
- **Excessive hedging.** could potentially possibly be argued that it might becomes may.
- **Generic conclusions.** The future looks bright. State specific plans or facts.

## Jargon and plain speech
- **Abstract metaphor nouns.** substrate, wedge, vector, locus, nexus, bedrock, scaffolding (as metaphor), paradigm, gold-plating, ratchet, evacuate, endgame, north star, flywheel. Use the concrete word.
- **Say what it does, not how it feels.** Name the mechanism or a number. If the sentence could appear unchanged in another project's docs, cut it.
- **Shorten or split dense sentences.** One idea per sentence.
- **Active voice.** Name the actor. Passive only when the actor is unknown or irrelevant.
- **Cut adverbs or use a stronger verb.** significantly improves becomes the measured delta.
- **Prefer the plain word.** utilize and leverage become use, facilitate becomes help.
- **Mannered prose.** No aphorisms, rhetorical fragments or personified code. Say what you mean.
- **Over-compression.** Do not drop articles or write verbless fragments. Spell out arrows and abbreviations.