# Release secrets and the release branches (Android)

Everything the two release workflows need, and nothing they can create for
themselves. The iOS repository has its own copy of this document covering the
App Store half; the two are deliberately separate, because the secrets are.

Until every secret below exists, both workflows stop on their first step and
name the one that is missing. They never half-run.

## The branches

- `version/X.Y` is the release's work in progress. Fix and feature branches
  merge into it, it is allowed to be noisy, and it produces no store build.
- `pre-release` is permanent and not per version. Merging `version/X.Y` into it
  is the signal that a candidate is worth testing: that one push builds the
  bundle, uploads it to Play's internal track, and stops.
- There is no release branch. Production is reached by running **Promote to
  production** against a version code that is already on the internal track.
- After a release ships, `version/X.Y` merges into `main` for the record and the
  release is tagged there.

Create it once:

```sh
git fetch origin
git switch -c pre-release origin/main
git push -u origin pre-release
```

Bring it forward afterwards. `main` moves by squash merge, so `pre-release`
cannot fast-forward to it; merge it in. **Never reset `pre-release` to `main`.**
`versionCode` is the commit count, so throwing away the previous candidate's
history numbers the next candidate below one Play has already accepted, and Play
refuses every upload from then on. If the squash commit conflicts, keep what
`pre-release` already has — it is the same work, arriving a second time. Do this
in the same push as the next candidate, so the release line still gets exactly
one build per candidate rather than one for the merge and another for the
candidate:

```sh
git fetch origin
git switch pre-release
git merge --no-ff origin/main
git merge --no-ff origin/version/2.1
git push origin pre-release
```

## The production environment

**Settings → Environments → New environment → `production`**, then tick
**Required reviewers** and add yourself. The promotion job does not start, and
never sees the Play key, until someone approves it there, and GitHub keeps the
record of who did. Required reviewers are free on a public repository.

## The marketing version

The version a candidate carries: `1.1`, `2.0`, `1.0.2`. On Android it is
`versionName` in `app/build.gradle.kts`, committed on the release line rather
than set in GitHub. `versionCode` needs nothing; it is the commit count.

Change it on `version/X.Y` the moment a release line starts a new version,
before the first candidate of that version is built, and set the iOS
repository's `MARKETING_VERSION` variable to the same string in the same
sitting. It is the one number the two stores must agree on for one release of
one product, and nothing links or checks them.

## The secrets

All of these go in **Settings → Secrets and variables → Actions → New
repository secret**, with the name spelled exactly as below.

### `GOOGLE_SERVICES_JSON`

Already exists; the other workflows use it. The whole contents of
`app/google-services.json`.

### `ANDROID_KEYSTORE_BASE64`

The upload keystore — the `.jks` that `TRAINR_UPLOAD_STORE_FILE` points at in
your `~/.gradle/gradle.properties`. It stays out of the repository; CI decodes it
into a temporary directory for the length of one run.

```sh
base64 -i "$(grep '^TRAINR_UPLOAD_STORE_FILE=' ~/.gradle/gradle.properties | cut -d= -f2-)" | pbcopy
```

### `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`

The values of `TRAINR_UPLOAD_STORE_PASSWORD`, `TRAINR_UPLOAD_KEY_ALIAS` and
`TRAINR_UPLOAD_KEY_PASSWORD` from the same `~/.gradle/gradle.properties`.

### `ADMOB_APP_ID`

AdMob → **Apps** → Trainr (Android) → **App settings** → **App ID**. Looks like
`ca-app-pub-0000000000000000~0000000000`. Not secret, but a release build stops
without it rather than quietly shipping Google's test id, so CI has to carry it.

### `ADMOB_PLAN_BANNER_ID`

AdMob → **Apps** → Trainr (Android) → **Ad units** → the plan banner unit →
**Ad unit ID**. Looks like `ca-app-pub-0000000000000000/0000000000`.

### `PLAY_SERVICE_ACCOUNT_JSON`

How the workflows talk to Play. Paste the file's contents as they are — raw
JSON, not base64.

1. Google Cloud console → **IAM & Admin → Service accounts** → **Create service
   account** (name it `trainr-release`). No project roles are needed; the
   permissions that matter are granted in Play.
2. On that account, **Keys → Add key → Create new key → JSON**. The file
   downloads once.
3. Play Console → **Setup → API access**. Link the Cloud project if it is not
   linked, find the service account in the list and **Grant access**.
4. Give it **Release → Release apps to testing tracks** and **Release → Release
   to production, exclude devices, and use Play App Signing**, restricted to the
   Trainr app. Nothing else.
5. Paste the JSON file's contents into the secret.

```sh
pbcopy < ~/Downloads/trainr-release-*.json
```

Delete the downloaded file afterwards. A new key can always be generated; this
one cannot be read back.
