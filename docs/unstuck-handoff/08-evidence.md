# 08 · Evidence and provenance

Prepared 21 September 2026. These sources informed the prior research and are retained for rechecking in a new thread. The LFM card, ACSM update and Google Gemma page were reopened during handoff preparation. Other links are prior-research references, not a claim they were all freshly checked. Verify current releases/licenses before implementation.

## Repository evidence

Android baseline `53ef59f4aa289b687ef7320c5164f0129b395832`; iOS `2eb4eb9286a3c9edf319a8b3536ac493525ba448`.

- `docs/generation-contract.md`: current deterministic planning; inspect code/tests as final evidence.
- `docs/on-device-generation.md`, Outcome (2026-09-12): prior internal weekly-generation experiment, 15 profiles, Gemma 4 E2B q4_0 and Qwen3-0.6B. Rules beat/tied model selection; reported Apple M2 week medians 26.9 s and 15.2 s. These are existing internal results, not Unstuck or phone benchmarks. The historical plan below that outcome is superseded.
- `docs/programming-evidence.md`: existing programming rationale, to reconcile with professional review.
- `docs/store-products.md`: existing products.
- Source maps and asset manifest identify inspected native files. Paths can drift after these commits.

## Training and UX

| Source | Supports / limitation |
|---|---|
| [ACSM 2026 update](https://acsm.org/resistance-training-guidelines-update-2026/) | Broad goal-sensitive resistance training; not proof of prototype set counts |
| [Prescription systematic review/network meta-analysis](https://pmc.ncbi.nlm.nih.gov/articles/PMC10579494/) | Loading/volume and strength/hypertrophy; group results do not predict an individual |
| [WHO physical activity](https://www.who.int/news-room/fact-sheets/detail/physical-activity) | Broad health context, not substitution/triage rules |
| [NIDDK eating and activity](https://www.niddk.nih.gov/health-information/weight-management/adult-overweight-obesity/eating-physical-activity) | Weight-management context; workout performance alone is not fat-loss evidence |
| [NNG usability heuristics](https://www.nngroup.com/articles/ten-usability-heuristics/) | Control, visibility, recognition, error prevention/recovery |
| [NNG progressive disclosure](https://www.nngroup.com/articles/progressive-disclosure/) | Essential decision first, details on demand |

Feature policies are design/engineering inferences informed by research. No cited study tests Trainr Unstuck or proves its results/revenue. No professional review is claimed to have occurred.

## Models and runtime

| Source | Use |
|---|---|
| [LFM2.5-1.2B official card](https://huggingface.co/LiquidAI/LFM2.5-1.2B-Instruct) | Purpose, template, decoding; vendor evaluation, not Trainr data |
| [GGUF listing](https://huggingface.co/LiquidAI/LFM2.5-1.2B-Instruct-GGUF/tree/main) | Quantization/size; pin revision/hash |
| [Liquid license](https://huggingface.co/LiquidAI/LFM2.5-1.2B-Instruct/blob/main/LICENSE) | Exact commercial terms, needs release review |
| [Liquid introduction](https://www.liquid.ai/blog/introducing-lfm2-5-the-next-generation-of-on-device-ai) | Vendor benchmarks, not apples-to-apples ranking |
| [LFM2.5-2.6B](https://huggingface.co/LiquidAI/LFM2.5-2.6B) | Larger reasoning challenger |
| [Gemma 4 / LiteRT-LM](https://developers.google.com/edge/litert-lm/models/gemma-4) | Artifacts/runtime measurements; CPU-memory metric may omit GPU allocation |
| [LiteRT Android](https://developers.google.com/edge/litert-lm/android) | Pin exact APIs rather than historical examples |
| [LiteRT constraints](https://github.com/google-ai-edge/LiteRT-LM/blob/main/docs/api/cpp/constrained-decoding.md) | C++ constrained output; native language bindings need verification |
| [Qwen3.5-2B](https://huggingface.co/Qwen/Qwen3.5-2B) | Multilingual challenger |
| [Qwen LiteRT conversion](https://huggingface.co/litert-community/Qwen3.5-2B) | Runtime/template limitations |
| [llama.cpp Android](https://github.com/ggml-org/llama.cpp/blob/master/docs/android.md) | Integration starting point |
| [llama.cpp grammars](https://github.com/ggml-org/llama.cpp/blob/master/grammars/README.md) | Syntax constraints, not semantic correctness |

## Competitive context

Do not claim no other app has adaptation/coaching without fresh evidence. Official references: [Hevy Trainer](https://help.hevyapp.com/hc/en-us/articles/38385724273047-Hevy-Trainer-Explained-How-It-Builds-Your-Workout-Program), [Freeletics Quick Adapt](https://www.freeletics.com/en/blog/posts/quick-adapt/), [Alpha Progression](https://alphaprogression.com/en/blog/alpha-progression-guide), [Herc](https://www.herc.fit/), [Aurabase](https://aurabase.app/). These are comparison leads, not a complete audit or uniqueness guarantee. Demonstrate useful execution and transparent privacy rather than an unverified first-ever claim.
