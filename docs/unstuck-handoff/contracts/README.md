# Contract use and semantic validation

These Draft 2020-12 schemas are proposed wire contracts, not existing native APIs or production validators. Pin compatible schema support in both adapters. If a runtime grammar supports only a subset, generate a compatible decoding grammar and always run the full validator afterward.

`intent.schema.json` is untrusted model extraction. `proposal.schema.json` is trusted-app output only after deterministic policy checks. Never feed a model-produced proposal directly to persistence, even if it passes the schema. No-change and safety-route are separate coordinator outcomes.

## Checks beyond JSON schema

- Evidence quote must equal exact input[start:end], using Unicode code-point indices, end exclusive. Check end > start and within input; test emoji/combining characters on Kotlin and Swift.
- All extracted actionable facts need matching evidence or explicit structured UI input. No equipment inventory, physical diagnosis, or recurrence may be inferred from silence.
- Model concern absence never clears a direct pain selection. Conflicting inputs require clarification; unknown time scope is not actionable.
- MemoryCandidate only invites explicit user confirmation; it is not consent.
- Validate IDs against the current user/session/catalog and latest revision. Reject unknown/deleted IDs, unexpected duplicate set IDs, unsupported operation combinations and stale previews.
- Before snapshots must match eligible unperformed data exactly. After snapshots must satisfy reviewed policy. Model and UI cannot write actual values through this contract.
- Different replacement catalog keys retain separate load history; omit has null after; reduce must retain the same exercise identity/key and a valid strict subset of eligible sets; replace needs a validated candidate and valid new/retained set identity policy.
- PreservedPerformedSetIds must match the real performed snapshot; merely listing them does not prove preservation. Compare identities and values before/after transaction.
- Numeric null is unknown, never zero; load 0 can mean explicitly unloaded where a real catalog measure allows it. Reps versus seconds, target units, per-hand versus total load and equipment increments come from the existing catalog/policy.
- Technical schema limits (e.g. 1440 minutes) are parser bounds, not recommendations. Reviewed supported ranges can be narrower.
- Apply authorization and entitlement are checked by the coordinator/repository, never encoded as a model boolean.

Fixture proposal values and IDs are symbolic, deliberately not executable prescriptions. The invalid fixture must fail schema validation. The behavior corpus describes expected routing, not exact model wording or unrun pass results.
