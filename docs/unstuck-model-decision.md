# Unstuck local model: evaluation and decision

Run 23 September 2026 against the handoff's first candidate. This is the model
decision record section 06 of the handoff asks for. It records what was
measured, on what, and what follows from it.

**Decision: do not ship this candidate.** The interpreter stays bound to
`UnavailableInterpreter`, and the "Something else" route continues to offer the
structured choices. Nothing about the feature changes.

## What was measured

| | |
|---|---|
| Model | `LiquidAI/LFM2.5-1.2B-Instruct-GGUF`, `Q4_K_M` |
| Artifact | 730,895,168 bytes, downloaded from the official repository |
| Runtime | llama.cpp, Apple Silicon, Metal |
| Prompt | the frozen system instruction in `unstuck-handoff/06-local-model.md`, verbatim |
| Constraint | GBNF grammar generated from `contracts/intent.schema.json` |
| Sampling | temperature 0 |
| Corpus | all 24 cases in `unstuck-handoff/fixtures/behavior-cases.json` |
| Scoring | the same semantic rules `IntentValidator` enforces on both platforms |

Re-run it with `tools/unstuck-eval/evaluate.py`; the harness takes the handoff
directory as its argument and needs only `llama-cli` on the path.

## Results

| Contract | Parsed | Passed the validator | Intent correct |
|---|---|---|---|
| **The handoff's, with code-point evidence offsets** | 24/24 | **0/24** | 5/24 |
| Intent classification alone, no evidence | 24/24 | not applicable | 15/24 |
| Evidence as a quote verified by containment | 24/24 | 23/24 | 16/24 |
| **A ~25-line deterministic keyword baseline** | not applicable | not applicable | **22/24** |

Latency was never the problem: median 2.4 s, p95 2.7 s on an M-series Mac,
though a phone will be several times slower and that was not measured.

## What the numbers mean

**The grammar works and proves nothing.** Every answer was syntactically valid
JSON matching the schema. Not one survived the semantic checks. This is the
handoff's own warning made concrete: "Grammar-constrained JSON limits syntax;
it does not make a wrong interpretation safe or accurate."

**The model cannot produce evidence offsets.** Asked for exact code-point
positions, it returned things like `end: 1698632000` — a unix timestamp — for a
46-character sentence. This single requirement accounts for the whole gap
between 0/24 and 16/24, and it also wrecks the classification, because the
model spends its attention inventing numbers it cannot count.

**The most dangerous failure was a negation.** Given "I do not have a time
problem. The bench is taken.", the model under the full contract answered
`pain_concern`, and under the intent-only prompt answered `less_time` — reading
the sentence backwards and proposing to shorten a workout for someone who said
time was explicitly not the issue. The keyword baseline gets this right.

**A regex beats it.** 22/24 against 16/24 at the model's best, and 5/24 under
the contract the app actually enforces. This repeats the outcome of the
weekly-generation experiment recorded in `on-device-generation.md`, where
deterministic selection also beat the model candidates.

## What would change the answer

Two findings are worth keeping for whenever a candidate is re-evaluated.

**Replace offsets with containment.** Verifying evidence by checking that the
quoted text appears in the note, rather than that it sits at stated code-point
positions, moved validation from 0/24 to 23/24 and intent accuracy from 5/24 to
16/24. It is strictly weaker — a quote can be verified without knowing which
occurrence it came from — but it is verifiable, which is the property that
matters, and it is achievable by a model. Any future contract should do this.

**Judge a candidate against the keyword baseline, not against nothing.** The
handoff asks for this comparison and it is the one that decides. A model that
loses to twenty-five lines of regex is not worth 731 MB, a custom licence and a
download flow.

## Licence

The artifact is under Liquid's own licence, not Apache-2.0. Read in full: it
grants the usual rights, requires notices and change statements to be retained,
and limits **commercial use to entities under 10 million US dollars of annual
revenue**. Trainr is far below that threshold, so the licence permits this use
today. It is a condition to revisit if revenue ever approaches it, and
accepting it remains a decision for the project owner rather than a consequence
of this evaluation.

## Limits of this evaluation

Honest scope, so nobody reads more into it than it supports.

- 24 cases. The handoff asks for at least 300, stratified, with a held-out set.
- One model, one quantization, one prompt, one runtime. No other candidate was
  tried.
- Desktop only. No phone, no thermal or memory pressure, no cold-start cost.
- The keyword baseline was written after seeing the corpus, so it is an
  optimistic upper bound on a rule-based approach, not a shipped component.
- This measures interpretation, not coaching quality. Passing would not have
  been permission to enable anything.

None of those caveats rescue the candidate: a model that fails every case of
the contract the app enforces does not pass on a larger corpus.
