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

Gender is also collected. Play has no separate gender type; it falls under
**Personal info → Other personal info** if you want to be exhaustive. Like the
name, it is collected but **not shared** — the prompt never includes it.

### Why "shared" is Yes

Play counts a transfer to a third party as sharing. The profile goes to Google's
Gemini API, which is a third party even though it is also Google. Answering No
here would be the kind of thing that gets an app pulled later.

### What to say it is not

- No advertising or marketing purpose
- No analytics purpose (there is no Analytics or Crashlytics SDK in the build)
- No account management (there are no accounts)
- No location, contacts, photos, files, messages, calendar, audio or camera data
- No device or other identifiers collected by the app itself

App Check with Play Integrity attests the app and device to Google, but carries
no profile data and is a security measure rather than data collection.

## Privacy policy URL

`docs/privacy-policy.md` in this repository. To publish it free:

1. GitHub → **Settings → Pages**
2. Source: **Deploy from a branch**, branch **main**, folder **/docs**
3. Save, wait a minute, and the URL is:
   `https://jerichomagallanes.github.io/Trainr/privacy-policy`

Paste that into Play Console under **Policy → App content → Privacy policy**, and
into the Store listing as well.
