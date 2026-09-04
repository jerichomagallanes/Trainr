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

Five rows. For every one: **collected = yes, processed ephemerally = no,
required = yes**. Sharing and purpose differ per row, so they are in the table.

| Category | Type | Shared | Purpose | Why |
| --- | --- | --- | --- | --- |
| Personal info | **Name** | **No** | App functionality | Asked for in onboarding, never leaves the device. |
| Health and fitness | **Health info** | **Yes** | App functionality | Height, weight and injuries. Sent to Gemini to write the plan. |
| Health and fitness | **Fitness info** | **Yes** | App functionality | Goal, experience, equipment, schedule, and the sets and reps you log. Sent to Gemini so the next week progresses from the last. |
| App info and performance | **Crash logs** | **No** | Analytics | Stack traces, device state and the hand-written trail. Play defines the Analytics purpose as "monitoring app health, diagnose and fix bugs or crashes", which is exactly this and is not App functionality. |
| Device or other IDs | **Device or other IDs** | **No** | Analytics | The Crashlytics installation UUID, which tells one crash apart from the same crash twice. Play's definition of this type names Firebase installation IDs, so it is declarable. |

Gender is also collected. Play has no separate gender type; it falls under
**Personal info → Other personal info** if you want to be exhaustive. Like the
name, it is collected but **not shared** — the prompt never includes it.

### Why the Gemini rows are shared and the Crashlytics rows are not

Play defines sharing as "transferring user data collected from your app to a
third party", and exempts transfers to a **service provider**: "an entity that
processes user data on behalf of the developer and based on the developer's
instructions". The distinction Google draws is whether the recipient uses the
data for its own purposes.

**Crashlytics is a service provider.** It processes crash reports on our behalf
and for no purpose of its own, so those rows are collected but not shared.

**The free Gemini API is not**, and its own terms say so plainly. For the Unpaid
Services: "Google uses the content you submit to the Services and any generated
responses to provide, improve, and develop Google products and services", and
"Human reviewers may read, annotate, and process your API input and output."
That is use for Google's own purposes, which is the definition of a third party
rather than a service provider. So those rows are shared, and answering No would
be wrong.

**On the paid tier this reverses**: "Google doesn't use your prompts ... or
responses to improve our products". Moving to paid would make Gemini a service
provider, and those two rows would become collected-but-not-shared.

### What to say it is not

- No advertising or marketing purpose
- **No Google Analytics.** Crashlytics is in the build; the Analytics SDK is
  deliberately not, which is why the app still declares **no** advertising ID.
  Adding Analytics would pull in `play-services-measurement`, which declares
  `com.google.android.gms.permission.AD_ID` in its own library manifest and
  would make that declaration false. Nothing is given up: breadcrumbs are
  written by hand instead, which Firebase lists as a separate capability from
  Analytics-provided ones.
- No account management (there are no accounts)
- No location, contacts, photos, files, messages, calendar, audio or camera data

App Check with Play Integrity attests the app and device to Google, but carries
no profile data and is a security measure rather than data collection.

### Crash reports carry no profile data

Crashlytics is set up with **no user ID**. It does carry custom keys and logs —
a hand-written trail of screens visited and the walk through the Gemini models —
but nothing the client typed goes into it, and a test enforces that by running a
full profile through a failing generation and asserting none of its values
appear. Rejected answers record how many problems there were, never what they
said, because a validation message can quote the model's own text and that text
was written from the profile.

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
