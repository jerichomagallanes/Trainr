# Play Console answer sheet

Everything here is checked against the code, not assumed. Where a question turns
on something only you can answer, that is called out.

## Target audience and content

| Question | Answer |
| --- | --- |
| Target age groups | **13-15, 16-17, 18 and over** |
| Appeals to children? | **No** |
| Ads in the app? | **No** |

The app enforces a minimum age of 13 (`Constants.Workout.MIN_AGE`), so declaring
anything lower would contradict the app itself. 13 is also the line COPPA and
Play's Families policy draw around collecting personal data, which this app does.

## Data safety

### Does your app collect or share any of the required user data types?
**Yes.**

### Is all of the user data collected by your app encrypted in transit?
**Yes.** The only thing that leaves the device goes to Google's Gemini API over
HTTPS.

### Do you provide a way for users to request that their data is deleted?
**Yes.** Uninstalling the app deletes everything; Android's Clear storage does
the same without uninstalling. Nothing is held off the device to delete.

### Data types

Declare these three. For every one: **collected = yes, shared = yes, processed
ephemerally = no, required = yes, purpose = App functionality**.

| Category | Type | Why |
| --- | --- | --- |
| Personal info | **Name** | Asked for in onboarding. Collected, **not shared** — it never leaves the device. |
| Health and fitness | **Health info** | Height, weight and injuries. Sent to Gemini to write the plan. |
| Health and fitness | **Fitness info** | Goal, experience, equipment, schedule, and the sets and reps you log. Sent to Gemini so the next week progresses from the last. |
| App info and performance | **Crash logs** | Stack traces and device state when the app crashes. Purpose is **Analytics**, not App functionality: Play defines that purpose as "monitoring app health, diagnose and fix bugs or crashes". |
| Device or other IDs | **Device or other IDs** | The Crashlytics installation UUID, which tells one crash apart from the same crash twice. Play's own definition of this type names Firebase installation IDs, so it is declarable. Purpose **Analytics**. |

Gender is also collected. Play has no separate gender type; it falls under
**Personal info → Other personal info** if you want to be exhaustive. Like the
name, it is collected but **not shared** — the prompt never includes it.

### Why "shared" is Yes

Play counts a transfer to a third party as sharing. The profile goes to Google's
Gemini API, which is a third party even though it is also Google. Answering No
here would be the kind of thing that gets an app pulled later.

### What to say it is not

- No advertising or marketing purpose
- **No Google Analytics.** Crashlytics is in the build; the Analytics SDK is
  deliberately not, which is why the app still declares **no** advertising ID.
  Adding Analytics would pull in `play-services-measurement`, which declares
  `com.google.android.gms.permission.AD_ID` in its own library manifest and
  would make that declaration false. The only thing given up is Crashlytics
  breadcrumb logs.
- No account management (there are no accounts)
- No location, contacts, photos, files, messages, calendar, audio or camera data

App Check with Play Integrity attests the app and device to Google, but carries
no profile data and is a security measure rather than data collection.

### Crash reports carry no profile data

Crashlytics is set up with no custom keys, no custom logs and no user IDs, so a
crash report contains a stack trace, device state and the installation UUID, and
nothing about the client. Dev builds do not report at all
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
