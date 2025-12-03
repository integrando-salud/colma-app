# GEMINI.md

## Project Overview

This is an Android application named "colmaApp", built with Kotlin and Jetpack Compose.
The application allows users to log in, track health data, and engage with daily content. It features offline capabilities, storing data in a local SQLite database, and a synchronization mechanism to send the collected data to a remote server.

### Key Technologies:
- **UI:** Jetpack Compose
- **Language:** Kotlin
- **Database:** SQLite (using `sqlite-asset-helper` to bundle a pre-populated database)
- **Video Playback:** ExoPlayer
- **Build Tool:** Gradle

### Core Features:
- User authentication (login screen)
- Health data tracking for:
  - Glucose (`glucosa`)
  - Blood Pressure (`presion`)
  - Mood (`estado_animo`)
- Daily sessions/content ("Tema de hoy")
- Data synchronization with a remote server via a webhook.

## Building and Running

This is a standard Android Gradle project.

### Building the Project
To build the project from the command line, use the Gradle wrapper:
```bash
./gradlew build
```

### Assembling the APK
To generate the APK, run:
```bash
./gradlew assembleDebug
```
The output APK can be found in `app/build/outputs/apk/debug/`.

### Running the Application
The recommended way to run the application is to open the project in Android Studio and run it on an emulator or a connected device.

## Development Conventions

- The project follows a feature-based package structure (e.g., `features.login`, `features.sesiones`).
- It uses ViewModels (`LoginViewModel`) to separate UI logic from business logic.
- Jetpack Compose is used for building the UI, with a theme defined in `app/src/main/java/com/colma/colmaApp/ui/theme/`.
- The app uses a local SQLite database (`colmadb.db` in `app/src/main/assets/databases/`) for data storage and has a `DbHelper` class to manage database interactions.
- Data synchronization is handled in `MenuMain.kt`, which collects data from the local database and sends it to a hardcoded webhook URL.

## Development Workflow
- At the end of each request, run a build to check if the implementation is working correctly. Use the command: `./gradlew build`
