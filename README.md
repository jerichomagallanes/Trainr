# 🏋️ Trainr - Workout Planner

Trainr is an Android workout planner that writes your week from your goal, your equipment and your days, bends it when life gets in the way, and builds next week from what you actually did. Everything runs on the phone: the plan is generated on-device, and a small language model reads the free-text "Adjust today" note without anything leaving the device. Built with Kotlin and Jetpack Compose, it follows Clean Architecture principles.

[<img src="docs/assets/badges/google-play.png" alt="Get it on Google Play" height="56">](https://play.google.com/store/apps/details?id=com.jericx.trainr)

Website: [jerichomagallanes.github.io/Trainr](https://jerichomagallanes.github.io/Trainr/) · iOS version: [Trainr-iOS](https://github.com/jerichomagallanes/Trainr-iOS)

<img src="https://github.com/user-attachments/assets/2a2a90be-b406-48df-9358-17700b164570" width="300" alt="Trainr App Demo">

## 📋 Features

- **Personalized Onboarding**: Complete fitness assessment including age, gender, experience level, and body metrics
- **Custom Workout Plans**: Routines built around your fitness goals and available equipment, and progressed week to week from what you actually lifted
- **Equipment Adaptation**: Pick what you own from a pictured list (none, dumbbells, barbells, kettlebells, plates, machines, bands); the week is written with that, and only that
- **Goal-Oriented Training**: Specialized programs for weight loss, muscle gain, strength, endurance, and general fitness
- **Injury Considerations**: Safe workout modifications based on reported limitations
- **Adjust Today**: Change a workout on the day for the reasons people actually stop. Less time keeps the most relevant work; missing equipment finds a suitable alternative; "show me how" opens the how-to; "something hurts" pauses and decides what to do next; or say it in your own words and a language model running on the phone reads the note, shows what it understood, and applies the change only when you say so
- **Session Logging**: Record weight, reps and time per set, with last week's numbers shown beside each one
- **Two Ways to Finish**: Log the whole session at its prescribed numbers, or save what you have done and stop
- **Built-in Timer**: Count down a timed exercise without leaving the session
- **Video Tutorials**: A hand-checked YouTube demonstration for each exercise in the catalog
- **Rescheduling**: Long-press a session to drag it onto another weekday
- **Week by Week**: Generate the next week from what you actually lifted, repeat any past week for free, or rewrite the week you are in
- **Progress Tracking**: Every stored week with its dates, status and completion

## 💳 Trainr Pro

The first week is free. Pro unlocks every week after it, as a monthly or yearly subscription with a 7-day free trial, or a one-time lifetime purchase. Purchases go through RevenueCat; the free tier shows one AdMob banner on the plan screen behind Google's consent flow, and Pro removes it.

## 🎯 Fitness Goals Supported

- **Weight Loss**: Fat-burning focused routines
- **Muscle Gain**: Hypertrophy and mass-building programs
- **Strength Training**: Power and strength development
- **Endurance**: Cardiovascular and stamina improvement
- **General Fitness**: Overall health and wellness
- **Flexibility & Mobility**: Range of motion and injury prevention

## 🏠 Workout Environments

- **Home Workouts**: Bodyweight and minimal equipment routines
- **Gym Training**: Full equipment-based programs
- **Hybrid Approach**: Flexible combination of both environments

## 📚 Tech Stack

- **Kotlin**: Modern Android development language
- **Jetpack Compose**: Declarative UI toolkit
- **Clean Architecture**: Separation of concerns with Domain, Data, and Presentation layers
- **MVVM Pattern**: Model-View-ViewModel architecture
- **Dagger Hilt**: Dependency injection framework
- **KSP**: Kotlin Symbol Processing for Hilt and Room code generation
- **Room Database**: Local data persistence
- **kotlinx.serialization**: JSON serialization for Room type converters
- **Kotlin Coroutines & Flow**: Asynchronous programming and reactive streams
- **llama.cpp**: Runs LiquidAI LFM2.5-1.2B-Instruct on the device to read the free-text Adjust today note; the model is a one-time 730 MB download, verified by SHA-256
- **RevenueCat**: Subscriptions and the lifetime purchase, with Play Billing underneath
- **Google Mobile Ads**: One banner on the free tier, behind the User Messaging Platform consent flow
- **Material Design 3**: Modern UI components and theming
- **Navigation Compose**: Single-activity navigation between Compose screens
- **android-youtube-player**: In-app exercise demonstrations via the official IFrame Player API

## 🏗️ Architecture

The app follows Clean Architecture principles with three main layers:

- **Presentation Layer**: UI components, ViewModels, and Compose screens
- **Domain Layer**: Repository contracts and framework-independent domain models
- **Data Layer**: Repository implementations, local database, and data sources

Plan generation sits behind a single `PlanGenerator` interface that answers
`Generated`, `Offline` or `Failed`, never a plan the model did not write. Every
answer is validated before it is stored, and re-asked with the specific errors when
it does not hold.

Adjust today's free-text path goes through an `IntentInterpreter` that turns the
note into one of the structured reasons with evidence quoted from the note itself;
the structured reasons never touch the model. A phone without room for the model
download is told so, and the other reasons keep working.

## 🚀 Releases

Candidates are built once by GitHub Actions when `version/X.Y` is fast-forwarded
onto `pre-release` (`Test release` → Play internal track), promoted to production
by hand from the Play Console or the `Promote to production` workflow, and tagged
by the `Release` workflow once `main` carries what the store sells. Every tag has
a GitHub Release with the shipped bundle attached and the pull requests since the
previous release.

## 📱 Minimum Requirements

- Android API Level 24 (Android 7.0)
- Compile SDK 37, Target SDK 36
- Kotlin 2.4

## 🔒 Privacy

Your profile, your training history and anything you type into Adjust today stay on your phone. Trainr has no accounts and no server of its own. The full policy: [Privacy Policy](https://jerichomagallanes.github.io/Trainr/privacy-policy).

## 📄 Licence

Released under the [MIT Licence](LICENSE): use it, learn from it, build on it.
Please keep the copyright notice, and note there is no warranty of any kind.
