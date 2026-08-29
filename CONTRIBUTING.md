# Contributing to Trainr

Trainr is a solo project, but it is developed as though it weren't. Every change
reaches `main` through a pull request that has passed CI and a deliberate
self-review. The point isn't ceremony — it's that reviewing your own diff in a
separate context catches the debug log you forgot to delete, the hardcoded
string, and the half-finished branch of an `if`, well before they become
history.

---

## The workflow

### 1. Branch

`main` is protected: it accepts no direct pushes. Start from an up-to-date copy:

```bash
git switch main
git pull
git switch -c feat/workout-timer
```

Branch names are `<type>/<short-description>`, using the same types as commits:

| Prefix      | For                                             |
| ----------- | ----------------------------------------------- |
| `feat/`     | A new capability                                |
| `fix/`      | A bug fix                                       |
| `refactor/` | Restructuring with no behaviour change          |
| `test/`     | Tests only                                      |
| `docs/`     | Documentation only                              |
| `chore/`    | Build, tooling, dependencies                    |

### 2. Commit

Commits follow [Conventional Commits](https://www.conventionalcommits.org):
`<type>(<optional scope>): <imperative summary>`.

```
feat(onboarding): add rest-day preference step
fix(room): stop losing body metrics on locale change
```

**One short sentence, and nothing more.** No body, no trailers, no co-authors.
Describe *what changes*, not what you did, and keep it under ~72 characters. The
reasoning belongs in the PR description, where it stays readable — not spread
across a commit body that nobody reads again.

### 3. Open a draft PR early

Push and open the PR as soon as you have your first commit — not when you're
finished:

```bash
git push -u origin feat/workout-timer
gh pr create --draft --fill
```

An early draft gives you CI on every push and a place to think out loud while
the work is still malleable. Fill in the template as you go rather than
reconstructing it at the end.

### 4. Let CI run

Every PR runs three required checks and one advisory one:

| Check                        | What it does                                              | Blocks merge |
| ---------------------------- | --------------------------------------------------------- | ------------ |
| **Unit tests**               | `testDevDebugUnitTest`, annotated per failing test on the diff | Yes |
| **Android Lint**             | `lintDevDebug`, findings published to the Security tab    | Yes |
| **Build debug APK**          | `assembleDevDebug`, APK attached to the run               | Yes |
| **Instrumentation tests**    | Espresso/Compose tests on an emulator                     | No |
| **Minified build smoke**     | Launches an R8-minified build and checks it doesn't crash | No |

Instrumentation tests are advisory on purpose: emulators flake, and a flake
should never be the reason a correct change can't merge. Read the result, re-run
if it looks like an infrastructure failure, and investigate if it doesn't. They
also run nightly against `main`.

### 5. Self-review, then merge

Mark the PR ready, then **open the "Files changed" tab and read the whole diff**
before merging. Work through the checklist in the PR template — it exists
because the things it lists are the things that actually slip through.

Merge with **Squash and merge** (the only option enabled). The PR title becomes
the commit message on `main`, so make sure it reads as a good commit. The branch
deletes itself on merge.

---

## Running things locally

```bash
# Unit tests
./gradlew testDevDebugUnitTest

# Android Lint (HTML report at app/build/reports/lint-results-devDebug.html)
./gradlew lintDevDebug

# Instrumentation tests on the same emulator CI uses — Gradle downloads,
# boots and tears down the device for you; no AVD setup needed.
./gradlew ciAtdDevDebugAndroidTest

# Instrumentation tests on a device or emulator you already have running
./gradlew connectedDevDebugAndroidTest

# Everything CI checks, in one go
./gradlew testDevDebugUnitTest lintDevDebug assembleDevDebug

# Install a build minified with release's R8 rules, to check a keep rule
# isn't missing. Nothing else runs against R8 output.
./gradlew installDevMinified
```

## Build flavors

| Flavor | Application ID             | Use                          |
| ------ | -------------------------- | ---------------------------- |
| `dev`  | `com.jericx.trainr.dev`    | Day-to-day development, CI   |
| `sit`  | `com.jericx.trainr.sit`    | Integration testing          |
| `prod` | `com.jericx.trainr`        | Release                      |

All three install side by side, and every build except `prodRelease` is labelled
with its flavor on the launcher so you can tell them apart.

## Dependencies

Versions live in `gradle/libs.versions.toml` — never hardcode one in a
`build.gradle.kts`. Dependabot opens grouped update PRs every Monday; they go
through the same CI as anything else.

## Breaking glass

Branch protection allows an admin bypass, for the case where CI itself is broken
and you need to fix it. Using it should feel like a small failure, and the next
PR should explain why it was necessary.
