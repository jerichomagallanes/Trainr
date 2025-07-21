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
- **Progress Tracking**: Monitor your fitness journey over time
- **Dark Mode Support**: Comfortable viewing in low-light conditions

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
- **Room Database**: Local data persistence
- **Kotlin Coroutines & Flow**: Asynchronous programming and reactive streams
- **Material Design 3**: Modern UI components and theming
- **Navigation Compose**: Type-safe navigation

## 🏗️ Architecture

The app follows Clean Architecture principles with three main layers:

- **Presentation Layer**: UI components, ViewModels, and Compose screens
- **Domain Layer**: Business logic, use cases, and domain models
- **Data Layer**: Repository implementations, local database, and data sources

## 🚀 Getting Started

1. Clone the repository:
   ```bash
   git clone https://github.com/jerichojericx/Trainr.git
   ```

2. Open the project in Android Studio

3. Build and run the application on an Android device or emulator

## 🛠️ Build Variants

The project includes multiple build variants for different environments:

- **Dev**: Development build with debug features and mock data
- **SIT**: System Integration Testing environment
- **Prod**: Production-ready build

Each variant supports both debug and release build types for comprehensive testing and deployment flexibility.

## 📱 Minimum Requirements

- Android API Level 24 (Android 7.0)
- Target SDK 35
- Kotlin 1.9+

## 🏃‍♀️ Start Your Fitness Journey

Download Trainr today and let AI create the perfect workout plan tailored just for you!
