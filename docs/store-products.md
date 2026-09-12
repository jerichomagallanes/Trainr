# Store products

What has to exist in App Store Connect, the Play Console and RevenueCat for the
paywall to sell anything. The app itself hard-codes only two things: the
entitlement identifier and the RevenueCat public key. Everything else — prices,
periods, trials, which plans exist — is read from the store at runtime, so a
change here needs no release.

Both apps share one RevenueCat project, one entitlement and one offering.

## Identifiers

| | Apple (App Store Connect) | Google (Play Console) |
| --- | --- | --- |
| Monthly | auto-renewable subscription `trainr_pro_monthly`, 1 month | subscription `trainr_pro`, base plan `monthly` (P1M) |
| Yearly | auto-renewable subscription `trainr_pro_yearly`, 1 year | subscription `trainr_pro`, base plan `yearly` (P1Y) |
| Lifetime | non-consumable in-app purchase `trainr_pro_lifetime` | one-time in-app product `trainr_pro_lifetime` |

Both subscriptions sit in one subscription group, **Trainr Pro**, so a buyer can
move between them and the store handles proration.

RevenueCat:

- Entitlement: `trainr_ai_workout_plans_pro` (the name predates the on-device
  generator; it is an internal id, so it was left alone rather than migrated).
  All three products attach to it.
- Offering: `default`, current. Packages `$rc_monthly`, `$rc_annual` and
  `$rc_lifetime`, each pointing at the matching product on both stores.
- App keys: the iOS app's public key (`appl_…`) lives in
  `Trainr-iOS/Trainr/Services/Purchases/Entitlements.swift`, the Android app's
  (`goog_…`) in `app/src/main/java/com/jericx/trainr/data/purchases/Entitlements.kt`.
  Both are public SDK keys and ship in the binaries. A release build that still
  held a Test Store key (`test_…`) would refuse to configure purchases and leave
  every paid path free rather than take money it cannot verify.

Package types drive the copy: `$rc_lifetime` shows "Pay once" and "Unlock Pro
forever", hides the renewal disclosure, and the Pro screen for its buyer says
"Pro is yours for good" with no "Manage subscription" link. The "Save N%"
badge on Yearly is computed from Monthly × 12 against the Yearly price the
store returns. A free trial appears in the button copy automatically when the
store product carries an introductory offer (Apple) or a free-trial phase on
the base plan's offer (Google).

## App Store Connect checklist

1. **Agreements, Tax, and Banking** — Paid Apps agreement active, bank and tax
   forms accepted. (Done.)
2. **Subscriptions** → create group **Trainr Pro** → add `trainr_pro_monthly`
   (1 month) and `trainr_pro_yearly` (1 year). Each needs a reference name, a
   price in the base storefront, a localised display name and description, and
   a review screenshot of the paywall.
3. **In-App Purchases** → Non-Consumable `trainr_pro_lifetime`, with the same
   metadata and screenshot.
4. **Users and Access → Integrations → In-App Purchase** — generate an In-App
   Purchase key (`.p8`). Note the Key ID and Issuer ID. RevenueCat needs all
   three to validate StoreKit 2 receipts.
5. **App → App Information → License Agreement** — paste `docs/terms-of-use.md`,
   otherwise Apple's standard EULA governs and the terms shown in the app are
   not the ones that apply.
6. On the first submission that sells Pro, attach all three products to the
   version under **In-App Purchases and Subscriptions**; they are reviewed with
   the build and cannot go live before it.

Sandbox: **Users and Access → Sandbox → Testers**; sign in with that Apple ID
on a device (Settings → App Store → Sandbox Account), never with a real one.

## Play Console checklist

1. **Setup → Payments profile** linked; **Monetise → Products** unlocked once a
   build with the `com.android.vending.BILLING` permission (the RevenueCat SDK
   adds it) has been uploaded to any track, internal testing is enough.
2. **Subscriptions** → `trainr_pro` → base plans `monthly` (1 month,
   auto-renewing) and `yearly` (1 year, auto-renewing), each activated with a
   price.
3. **In-app products** → `trainr_pro_lifetime`, one-time, activated.
4. **Setup → API access** — a Google Cloud service account with *Financial data*
   and *Manage orders* permissions on this app; its JSON key goes to
   RevenueCat. Real-time developer notifications need a Pub/Sub topic, which
   RevenueCat's Play setup page walks through.

Testing: add the Google account to **Setup → License testing** so purchases in
the internal-testing build are not charged.

## RevenueCat checklist

1. Project → **Apps** → add *App Store* (bundle id `com.jericx.trainr`, the
   `.p8` key, Key ID, Issuer ID) and *Play Store* (package `com.jericx.trainr`,
   the service-account JSON).
2. **Products** → import or add the six store products above.
3. **Entitlements** → `trainr_ai_workout_plans_pro` → attach all six.
4. **Offerings** → `default` → packages `$rc_monthly`, `$rc_annual`,
   `$rc_lifetime`, each with its Apple and Google product. Mark the offering
   current.
5. Copy each app's public SDK key into the file named above and ship.
