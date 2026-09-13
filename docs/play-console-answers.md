# Play Console answer sheet

Everything here is checked against the code, not assumed. Where a question turns
on something only you can answer, that is called out.

## Target audience and content

| Question | Answer |
| --- | --- |
| Target age groups | **13-15, 16-17, 18 and over** |
| Appeals to children? | **No** |
| Ads in the app? | **Yes** — one AdMob banner on the weekly plan for the free tier; none with Pro |

The app enforces a minimum age of 13 (`Constants.Workout.MIN_AGE`), so declaring
anything lower would contradict the app itself. 13 is also the line COPPA and
Play's Families policy draw around collecting personal data, which this app does.

## Data safety

### Does your app collect or share any of the required user data types?
**Yes.**

### Is all of the user data collected by your app encrypted in transit?
**Yes.** Everything that leaves the device goes over HTTPS: crash reports to
Firebase Crashlytics, purchase receipts to RevenueCat, and ad requests to
Google AdMob.

### Do you provide a way for users to request that their data is deleted?
**Yes.** Uninstalling the app deletes everything; Android's Clear storage does
the same without uninstalling. Nothing is held off the device to delete.

### Data types

Five rows. Play defines **Collected** as "transmitted off the user's device", and
its exemptions say data processed only on the device is not collected at all,
so the name, gender, measurements, goals, equipment, schedule and logged sets
are **not declared**: they never leave the phone. (An earlier version of this
sheet declared them anyway; the console's own definition, shown beside the
checkbox, says not to.) For the Crashlytics and RevenueCat rows: **collected =
yes, shared = no, processed ephemerally = no, required = yes**. The three AdMob
rows are **shared = yes**,
because Google uses them for its own advertising business, not only on our
behalf; Google's own answer sheet for its SDK is at
https://developers.google.com/admob/android/privacy/play-data-disclosure and
these rows follow it. Purpose differs per row, so it is in the table.

| Category | Type | Shared | Purpose | Why |
| --- | --- | --- | --- | --- |
| App info and performance | **Crash logs** | **No** | Analytics | Stack traces, device state and the hand-written trail. Play defines the Analytics purpose as "monitoring app health, diagnose and fix bugs or crashes", which is exactly this and is not App functionality. |
| Financial info | **Purchase history** | **No** | App functionality, Analytics | Trainr Pro purchases, verified through RevenueCat, which is a service provider and keeps them so a reinstall can restore Pro. RevenueCat's own guidance names exactly this row and no other: https://www.revenuecat.com/docs/platform-resources/google-platform-resources/google-plays-data-safety |
| Device or other IDs | **Device or other IDs** | **No** | Analytics | The Crashlytics installation UUID, which tells one crash apart from the same crash twice. Play's definition of this type names Firebase installation IDs, so it is declarable. |
| Device or other IDs | **Device or other IDs** | **Yes** | Advertising or marketing | The advertising ID, read by the AdMob SDK for the one banner on the free tier's weekly plan. The SDK adds `com.google.android.gms.permission.AD_ID` to the manifest itself. |
| Location | **Approximate location** | **Yes** | Advertising or marketing, Analytics | Inferred by Google from the IP address of the ad request; the app never asks for location permission. |
| App activity | **App interactions** | **Yes** | Advertising or marketing, Analytics | Ad impressions and taps on the banner, which is what AdMob measures to pay out. |

### Why no row is shared

Play defines sharing as "transferring user data collected from your app to a
third party", and exempts transfers to a **service provider**: "an entity that
processes user data on behalf of the developer and based on the developer's
instructions". The distinction Google draws is whether the recipient uses the
data for its own purposes.

**Crashlytics and RevenueCat are service providers.** They process crash reports
and purchase receipts on our behalf and for no purpose of their own, so those
rows are collected but not shared. **AdMob is not**: Google uses ad data for its
own advertising business, which is why its three rows are shared. The profile
and training data never leave the device at all, so they have no row: the plan
is built by the app from the bundled catalog.

### What to say it is not

- No advertising purpose beyond the AdMob rows above: Trainr's own data (the
  profile, the plan, the logged sets) is never used for ads and never reaches
  the ad SDK.
- **No Google Analytics.** Crashlytics is in the build; the Analytics SDK is
  deliberately not. Breadcrumbs are written by hand instead, which Firebase
  lists as a separate capability from Analytics-provided ones. The advertising
  ID is now declared because the AdMob SDK reads it, not because of Analytics.
- No account management (there are no accounts)
- No location, contacts, photos, files, messages, calendar, audio or camera data

### Crash reports carry no profile data

Crashlytics is set up with **no user ID**. It does carry custom keys and logs —
a hand-written trail of screens visited (route patterns, never their arguments)
and purchase events — but nothing the client typed goes into it.

Dev builds do not report at all
(`firebase_crashlytics_collection_enabled` is false in the dev manifest).

Verify the AD_ID claim at any time:

    ./gradlew :app:processProdReleaseManifestForPackage
    grep -c AD_ID app/build/intermediates/packaged_manifests/prodRelease/*/AndroidManifest.xml

## Privacy policy URL

`docs/privacy-policy.md` in this repository. To publish it free:

1. GitHub → **Settings → Pages**
2. Source: **Deploy from a branch**, branch **main**, folder **/docs**
3. Save, wait a minute, and the URL is:
   `https://jerichomagallanes.github.io/Trainr/privacy-policy`

Paste that into Play Console under **Policy → App content → Privacy policy**, and
into the Store listing as well.
