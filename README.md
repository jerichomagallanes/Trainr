# 🏋️ Trainr - AI-Powered Personal Training App

Trainr is an Android fitness application that creates personalized workout routines using AI. Built with Kotlin and Jetpack Compose, it follows Clean Architecture principles to provide users with customized training plans based on their individual fitness goals, experience level, and available equipment.

App Demo

<img src="https://github.com/user-attachments/assets/4cd6258d-f9b7-4db6-97d7-9d79c03b9902" width="300" alt="Trainr App Demo">


## 📋 Features

- **Personalized Onboarding**: Complete fitness assessment including age, gender, experience level, and body metrics
- **Custom Workout Plans**: AI-generated routines tailored to your fitness goals and available equipment
- **Flexible Setup**: Support for home, gym, or hybrid workout environments
- **Equipment Adaptation**: Workouts adapt to your available equipment (bodyweight, dumbbells, barbells, etc.)
- **Goal-Oriented Training**: Specialized programs for weight loss, muscle gain, strength, endurance, and general fitness
- **Injury Considerations**: Safe workout modifications based on reported limitations
- **Session Logging**: Record weight, reps and time per set, with last week's numbers shown beside each one
- **Built-in Timer**: Count down a timed exercise without leaving the session
- **Video Tutorials**: A hand-checked YouTube demonstration for each exercise in the catalog
- **Rescheduling**: Long-press a session to drag it onto another weekday; days you have missed are derived from the calendar, never stored
- **Week by Week**: Generate the next week from what you actually lifted, run any past week again, or rebuild the week you are in
- **Progress Tracking**: Every stored week with its dates, status and completion

### Not yet

- **Dark mode**: the theme exists but the app is pinned to light — the design has no dark variant yet
- **Japanese and Tagalog**: the translations are in the repository but excluded from the build; the app forces English so dates and strings cannot disagree

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
- **Material Design 3**: Modern UI components and theming
- **Navigation Compose**: Single-activity navigation between Compose screens
- **Firebase AI Logic**: Gemini generation without an API key in the app
- **Firebase App Check**: Play Integrity attestation, so only the real app can spend the quota
- **android-youtube-player**: Official IFrame Player API, bound to the screen's lifecycle

## 🏗️ Architecture

The app follows Clean Architecture principles with three main layers:

- **Presentation Layer**: UI components, ViewModels, and Compose screens
- **Domain Layer**: Repository contracts and framework-independent domain models
- **Data Layer**: Repository implementations, local database, and data sources

Plan generation sits behind a single `PlanGenerator` interface returning
`Generated`, `Offline` or `Failed` — never a plan the model did not write. The
Firebase-backed implementation walks an ordered list of Gemini models, because the
free allowance is counted per model: one that has run out for the day hands over to
the next without spending a retry. Every answer is validated against the generation
contract and re-asked with the specific errors when it does not hold.

## 🚀 Getting Started

1. Clone the repository:
   ```bash
   git clone https://github.com/jerichomagallanes/Trainr.git
   ```

2. Open the project in Android Studio

3. **Add a Firebase project.** The build fails without it — `File google-services.json is missing`.

   - Create a project at [console.firebase.google.com](https://console.firebase.google.com)
   - Register an Android app for `com.jericx.trainr` and another for `com.jericx.trainr.dev`
   - Enable **AI Services → AI Logic** and choose the **Gemini Developer API** provider
   - Download `google-services.json` and put it at `app/google-services.json`

   The file is deliberately not in this repository, so a clone has to bring its own.

4. **Register an App Check debug token.** App Check is enforced, and nothing installed
   by adb can pass Play Integrity. Run the app once, take the token from logcat
   (`Firebase App Check debug token: …`), and add it under **App Check → your app →
   Manage debug tokens**. Without this, generation is refused.

5. Build and run the application on an Android device or emulator

## 🛠️ Build Variants

The project includes multiple build variants for different environments:

- **Dev**: Development build, the one CI exercises
- **SIT**: System Integration Testing environment — currently has no Firebase app registered, so it does not build
- **Prod**: Production-ready build

Alongside `debug` and `release` there is a **minified** build type: release's R8
configuration with a debug signature, so CI can launch a shrunk build and catch a
missing keep rule before production does.

## 📱 Minimum Requirements

- Android API Level 24 (Android 7.0)
- Compile SDK 37, Target SDK 36
- Kotlin 2.3+

## 🏃‍♀️ Start Your Fitness Journey

Download Trainr today and let AI create the perfect workout plan tailored just for you!

## 📄 Licence

Released under the [MIT Licence](LICENSE) — use it, learn from it, build on it.
Please keep the copyright notice, and note there is no warranty of any kind.
