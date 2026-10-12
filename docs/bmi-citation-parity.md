# Android BMI citation parity

This change mirrors [Trainr-iOS PR #122](https://github.com/jerichomagallanes/Trainr-iOS/pull/122), which addresses Apple's October 11 rejection of iOS 1.0 (88) for BMI calculations without easily discoverable citations.

## Behavior

- Your Measurements exposes **About BMI & sources** before any measurements are entered, including when editing a saved profile.
- Every displayed adult BMI result includes its formula, **CDC: BMI calculation**, and **CDC: Adult BMI categories** as direct external links.
- The sheet groups calculation, adult ranges, and limitations with their respective citations. Its Close action stays visible while the explanation scrolls.
- Source controls have at least a 48 dp tap target, wrap, use Trainr's existing typography/colors, and expose button semantics. Section headings have heading semantics.
- Age-20 eligibility, accepted measurements, conversion, rounding, calculation, and category thresholds remain unchanged. Source explanations are available to younger users without giving them an adult BMI classification.
- Explanations are bundled for offline reading. CDC pages need connectivity. Source URLs contain no profile data.

## Sources

- [CDC: About Body Mass Index](https://www.cdc.gov/bmi/about/index.html): formula and limitations.
- [CDC: Adult BMI Categories](https://www.cdc.gov/bmi/adult-calculator/bmi-categories.html): adult age boundary and category ranges.

These are the same destinations used by iOS. Source review date: October 12, 2026.

## Validation

- 482 unit tests passed, with no failures or skips.
- Android Lint, debug APK, and instrumentation APK builds passed.
- All 22 Body Metrics instrumentation tests passed on the existing Trainr_E2E Android 16 emulator, including pre-input access, exact destination interception, input preservation after dismissing the sheet, editing, and teenage result suppression.
- The dark-theme citation check also passed with the emulator's actual system font scale set to 2.0. The original 1.0 setting was restored afterward. A composition-local font override was insufficient to exercise text scaling inside the separate modal window, so the evidence uses the real system setting.
- [Screenshots](review-evidence/) were visually inspected in light mode, dark mode, and at 200% system text. Fixtures contain canned values.

## Release and submission

Build the production variant with `./gradlew bundleProdRelease assembleProdRelease`. This release is **1.2 (167)**. The earlier **1.1 (166)** upload was withdrawn from review to correct the public version name. Its version code is already consumed by Play, so the commit-count versioning now has an offset of 1 and a floor of 167. This PR remains a single commit: the branch and squash-merged main both have 166 commits and produce code 167; the following merge produces code 168. The floor also protects shallow or exported builds.

The upload keystore and production AdMob properties are read from the existing local Gradle configuration; they must never be committed. Verify the bundle signature, version, production identifiers, and package `com.jericx.trainr` before uploading. Retain the matching R8 mapping file for the release.

For review: start onboarding with age 20 or older, open Your Measurements, and open About BMI & sources before entering values. Enter 175 cm and 72 kg, then scroll to the result and direct source links. Reopen Measurements through Edit on the fitness profile to verify the same access. No purchase is needed.

Release note (en-US): “• BMI sources are now easier to find, right beside your results”

The PR and local build do not upload or roll out an update. Submit the new Android bundle as an update to the existing production app; its current production release is already live. The iOS replacement build is handled separately through App Store Connect.
