# 🏋️ Trainr - AI-Powered Personal Training App

Trainr is an Android fitness application that creates personalized workout routines using AI. Built with Kotlin and Jetpack Compose, it follows Clean Architecture principles to provide users with customized training plans based on their individual fitness goals, experience level, and available equipment.

App Demo

<img src="https://github.com/user-attachments/assets/2a2a90be-b406-48df-9358-17700b164570" width="300" alt="Trainr App Demo">

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
- **Rescheduling**: Long-press a session to drag it onto another weekday
- **Week by Week**: Generate the next week from what you actually lifted, run any past week again, or rebuild the week you are in
- **Progress Tracking**: Every stored week with its dates, status and completion

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
- **Firebase App Check**: Play Integrity attestation for every generation request
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

## 📱 Minimum Requirements

- Android API Level 24 (Android 7.0)
- Compile SDK 37, Target SDK 36
- Kotlin 2.4

## 🏃‍♀️ Start Your Fitness Journey

Download Trainr today and let AI create the perfect workout plan tailored just for you!

## 📄 Licence

Released under the [MIT Licence](LICENSE): use it, learn from it, build on it.
Please keep the copyright notice, and note there is no warranty of any kind.
