# Handoff verification record

Prepared 21 September 2026. This records what was checked for this documentation/design delivery, not the future native feature.

## Completed checks

- Android and iOS baseline commits read; current model, generation, persistence, purchase and design integration points inspected.
- Original prototype HTML/JS, logo assets and fonts copied into this package. Frozen HTML/JS remain separate from the deterministic capture harness.
- 26 fixture scenarios rendered in both light and dark themes. DOM scene/theme/heading checked before each of 52 PNG exports. Every reference is 390 × 844 pixels; capture log included. Browser-supplied JPEG bytes were normalized to PNG without resizing; exact colors/geometry come from source tokens/CSS, not compressed raster samples.
- Both contact sheets visually inspected for framing, distinct states, light/dark colors, major hierarchy and action placement. These show the initial viewport; scroll/disclosure content remains in the interactive source.
- Browser smoke flow: start workout → reason chooser → less time → review → apply → adjusted banner → undo → finish early → save → truthful simulated partial confirmation. No browser console errors reported in the checked flow.
- JSON files parsed, local Markdown/HTML links checked, screenshot dimensions checked, valid intent/proposal fixture shapes checked, extra-key fixture rejected, evidence substring verified using the bundled lightweight checker.
- Font files compared to native source bytes; logo SVG path/fill data compared to existing Android vector sources; frozen prototype files compared to original source.
- SHA-256 file manifest and portable ZIP generated; archive integrity and expected contents checked.

## Deliberate limits

No Android/iOS application code was changed. No native builds, migration tests, actual model inference, performance benchmarks, clinical review, licensing approval, payment transactions, or native pixel-parity tests were run for this handoff. The acceptance scenarios are requirements, not passing production test results.

`verify_package.py` uses the schema vocabulary needed by these examples for a lightweight shape check. It is not a complete Draft 2020-12 implementation or a production validator. Native implementation must use a suitable validator and all semantic checks in contracts/README.md. Syntax/schema validity does not establish coaching validity.

The prototype is simulation-only; examples deliberately do not prove scientific timing or efficacy. Pain escalation and brand-label contrast are explicit release gates. A clean console does not prove accessibility or clinical safety.

## Reproduce package integrity checks

From the unpacked folder, run `python3 verify_package.py` with Python 3. It requires only the standard library and does not modify app data or contact the network. The manifest excludes itself. If intentionally editing the package, regenerate the manifest and record a new package version; do not present altered files as the frozen reference.
