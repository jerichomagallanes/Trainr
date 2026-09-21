# 04 · Native UI design handoff

## What “match the prototype” means

Use the bundled HTML/CSS/JS and screenshots as the visual reference. Reproduce the same information hierarchy, spacing, card geometry, restrained orange accents, Rubik headings, light/dark palette, wording hierarchy, and action placement. Implement with native Compose and SwiftUI components, not an embedded WebView. Keep existing app screens/components consistent. Do not replace the design with generic chat bubbles, gradients, floating AI mascots, dashboards, or a new bottom tab.

The frozen `prototype/index.html` and `prototype/prototype.js` are the original interactive design. `capture.html` adds only deterministic fixtures and fixed framing for screenshot export. `scenes.json` records inputs. Capture fixtures must never enter app production code. The gallery includes both theme variants and key scenario differences. When a screen's content exceeds the viewport, the screenshot shows its initial viewport; the prototype remains the reference for scroll/disclosure contents.

## Coordinate and layout specification

Reference phone: **390 × 844 CSS pixels**, including 1 px outer preview border, fake 28 px status row and 64 px top bar. On a real phone use real system insets; do not draw the fake status bar, phone border, or rounded device frame. Interior content width in the reference is 348 px (390 − 2 border − 40 horizontal padding).

| Element | Reference |
|---|---|
| Content | Horizontal 20; top 16; bottom 24; flexible vertical scroll |
| Top bar | 64 high in mockup; logo 32 high centered; left/right touch areas 44 |
| Footer | Background page; top divider 1 sunken; padding 16 vertical/20 horizontal; row gap 10; fixed below scroll |
| Primary | Min-height 56; radius 10; horizontal/vertical padding 14 in web; uppercase 16 bold; brandLarge fill |
| Secondary | Min-height 56; radius 10; outline 2 ink; page fill; action-colored text |
| Quiet action | Min-height 44; centered; muted; full width where secondary footer action |
| Cards | Outline 1 control color; radius 10; 15 internal padding; vertical margin 16 |
| Exercise card head | 15 padding; emphasis background; white text |
| Option rows | Min-height 56; padding 14 vertical/15 horizontal; radius 10; outline 1; margin 10 vertical |
| Selected option | Outline 2 action; reduce padding by 1 to avoid jump |
| Time chips | Gap 8; radius 8; padding 10 vertical/12 horizontal; min-height 44; flex to available width |
| Scope row | Sunken background; padding 10 vertical/12 horizontal; radius 8; gap 6; top margin 16 |
| Tradeoff | Left border 3 brandLarge; left padding 12; vertical padding 2; vertical margin 16 |
| Disclosure | Top rule 1; margin-top 16; label padding 12 vertical; min-height 44 |
| Before/after row | Two columns, flexible label plus trailing value; gap 10; padding 12 vertical; bottom sunken rule |
| Banner | Sunken background; padding 12 vertical/15 horizontal; radius 8; vertical margin 12 |
| Text area | Min-height 100 reference; radius 8; border 1; padding 12; grows with content/accessibility |

Do not use fixed viewport height for native content. Footer must clear navigation/home indicators and IME. At narrow widths, wrap scope/details and reduce neither touch targets nor readable text. On tablets use the app's existing bounded content scaffold; do not stretch a two-column diff across the whole screen.

## Type and color

`design-tokens.json` is the machine-readable feature token reference. It includes semantic theme colors, type roles, spacing and radii. Actual fonts `rubik_regular.ttf`, `rubik_bold.ttf` and logos are bundled. Native apps already have their own assets; verify names/checksums and reuse them instead of duplicating fonts.

Feature title is **Rubik Bold 20/28**, an intentional feature-local role. Existing native screen titles can remain 16/24 outside Unstuck. Priority text is Rubik Bold 16/24. Feature body matches web reference **14/21**, with system body family (Arial is a browser surrogate, not a font to ship). Supporting card headings are system bold 16/24; eyebrow 12/18; scope label 13 bold; actions system heavy 16. Existing set-entry text keeps the native component's typography. Use scalable units on both platforms.

Dark mode is not a simple inversion. Orange changes from #D37200 to #E8963A for large feature actions; on-brand text changes from white to #101519. Read exact semantic tokens. In light mode text links use #AB5C00 rather than the brighter fill orange. Selected chips use dark ink/white in light mode and pale #E4EAEC/dark #101519 in dark mode.

**Accessibility release gate:** the reference's white 16 px primary label on #D37200 needs contrast review; do not claim the reference is WCAG-compliant. If the actual weight/size does not qualify for a large-text threshold, use the existing stronger brand role or another reviewed accessible treatment and document the small visual deviation in a new reference. Do not silently reduce font sizes or ignore the issue to achieve a screenshot match. Validate both theme palettes and disabled/error states.

## Native component mapping

| Role | Android | iOS |
|---|---|---|
| Theme | `presentation/common/theme/{Color,Theme,Typography,Font,Spacing,Shape,ComponentHeight,Animation}.kt` | `Trainr/DesignSystem/{Colors,Typography,TextStyles,Spacing}.swift` |
| Scaffold/top bar | Existing `TrainrTopBar` and feature screen layout | `ScreenScaffold`, `ScreenContent`, existing navigation treatment |
| Primary action | Existing `TrainrButton`, feature label role if necessary | `PrimaryButton` |
| Choice controls | Existing selection card/toggle chip controls | `SelectionCard`, `Chips`, `PillButton`, `RadioDot` |
| Input | Existing Trainr text field | `AppTextField`, `FieldError` |
| Workout content | Existing exercise card/set table/timer/how-to components | `Features/Workouts/Components` exercise card/set table/tutorial/timer |
| Pro | Existing Pro prompt/paywall | `Features/Purchases/{ProPromptSheet,ProPaywallView,ProStatusView}.swift` |

Map semantic roles, not guessed exact APIs. Locate current component signatures before implementation. iOS text modifiers already scale using `@ScaledMetric`; avoid applying a second dynamic-type multiplier. Android uses dp/sp rather than screenshot pixels; iOS uses points. Compare at the same logical size and default font scale, then separately test accessibility scaling.

## Visual QA protocol

1. Build deterministic native preview fixtures matching `scenes.json`, using fake services, never live user data.
2. Capture at 390×844 logical reference size where available; normalize for real status/safe-area differences by comparing content region separately. Also test representative supported small and large devices.
3. Compare light and dark: title baseline, 20-unit gutters, cards, action dimensions, disclosure order, selected states, logo size, footer placement, text wrapping.
4. Use side-by-side and translucent overlays; review typography rasterization differences manually. A proposed geometry tolerance is 2 logical units for deliberate layout dimensions; this is a review target, not a claim native font glyphs are pixel-identical.
5. Record deviations in an explicit table: reference, native behavior, rationale, reviewer. Real OS insets, native icons, system body font, accessible text growth, tutorial embedding, and reviewed safety/purchase copy are allowed with documentation. Unapproved reorganization is not.
6. Test scroll, disclosure expansion, keyboard, back, sheet dismissal, loading, failed apply, undo, and dynamic type. A static screenshot cannot certify these.

## Assets and licensing

Logos were derived from existing Trainr Android vector paths; fonts copied from the app's bundled fonts. They are project assets, not newly licensed stock assets. Keep existing font/software notices and verify distribution rights from the repository before repackaging. `asset-provenance.json` records source paths and baseline. Never fabricate a Figma file, design node ID, or asset license; no Figma document was created for this handoff.
