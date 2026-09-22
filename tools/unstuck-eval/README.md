# Unstuck interpreter evaluation

Runs a GGUF model over the handoff's behaviour corpus and scores every answer
with the same semantic rules `IntentValidator` enforces in both apps, so a
candidate is judged on what the app would actually accept rather than on
whether it produced JSON.

```
brew install llama.cpp
curl -L -o model.gguf <the candidate's GGUF url>
python3 evaluate.py ../../docs/unstuck-handoff
```

`evaluate.py` expects the model beside it as `lfm25-q4km.gguf`; change `MODEL`
for another candidate. The grammars are the constraint variants the first
evaluation compared: `intent.gbnf` is the handoff's full contract,
`quote.gbnf` replaces code-point offsets with a quote verified by containment,
and `intent-only.gbnf` is classification alone.

Results of the first run, and what follows from them, are in
`docs/unstuck-model-decision.md`. `results.json` is that run's raw output.

Judge any candidate against the deterministic keyword baseline in the decision
record, not against nothing: the first one lost to it.
